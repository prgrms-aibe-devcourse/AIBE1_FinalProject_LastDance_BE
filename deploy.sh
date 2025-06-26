#!/usr/bin/env bash
###############################################################################
# Blue/Green 배포 스크립트 (EC2 내부 실행)
###############################################################################
set -euo pipefail
# ─── 고아파일 처리 로직 ─────────────────────
DEPLOY_OK=false        # 처음엔 실패로 가정

cleanup() {
  if [ "$DEPLOY_OK" = false ]; then
    echo "[CLEANUP] 롤백/초기화…"
    $COMPOSE -f "$COMPOSE_FILE" rm -f "$NEW_APP_SERVICE" 2>/dev/null || true
  fi
}
trap cleanup EXIT      # <- 그대로

# ... 헬스체크 통과 + Nginx 전환까지 끝나면
DEPLOY_OK=true

# ────────── 0. 공통 변수 ────────────────────────────────────────────────────
PROJECT_NAME="lastdance-app"
APP_DIR="/home/ubuntu/${PROJECT_NAME}"
DOCKER_APP_DIR="${APP_DIR}/docker" # 앱 컨테이너의 docker-compose.yml 경로
MONITORING_DIR="${APP_DIR}/monitoring" # 모니터링 스택의 docker-compose.yml 경로


COMPOSE_FILE="${DOCKER_APP_DIR}/docker-compose.yml"   # ▶ ① 반복 경로 변수화
NGINX_CONF="/etc/nginx/sites-available/lastdance-app.conf"

BLUE_PORT=8080
GREEN_PORT=8081
BLUE_SERVICE="blue-app"
GREEN_SERVICE="green-app"

# docker-compose v1, v2 자동 감지
COMPOSE="docker-compose"
command -v docker compose &>/dev/null && COMPOSE="docker compose"

echo "────────── ECR 로그인 및 최신 앱 Docker 이미지 가져오기 ──────────────────"
# deploy.yml의 envs: 로부터 AWS_REGION, ECR_URI, ECR_REPO, IMAGE_TAG가 전달됩니다.
FULL_IMAGE_NAME="${ECR_URI}/${ECR_REPO}:${IMAGE_TAG}" # IMAGE_TAG는 github.sha

aws ecr get-login-password --region "${AWS_REGION}" | docker login --username AWS --password-stdin "${ECR_URI}" || { echo "ECR 로그인 실패! AWS CLI 또는 IAM 권한 문제."; exit 1; }
echo "▶ ECR 로그인 성공."

echo "▶ Docker 이미지 풀: ${FULL_IMAGE_NAME}"
docker pull "${FULL_IMAGE_NAME}" || { echo "Docker 이미지 풀 실패! 이미지가 존재하지 않거나 권한 문제."; exit 1; }
echo "▶ Docker 이미지 풀 완료."

# 공유 Docker 네트워크 생성 또는 확인
echo "[EC2] 공유 Docker 네트워크 'app-monitor-network' 생성 또는 확인..."
docker network create app-monitor-network || true # 이미 존재하면 오류 없이 넘어감
echo "▶ 네트워크 'app-monitor-network' 준비 완료."

# ────────── 1. 현재 활성 포트 파악(숫자만) ────────────────────────────────
echo "[1/5] 현재 활성화된 앱 포트 확인…"

# Nginx 설정 파일에서 'upstream current_app' 블록 바로 다음 줄의 포트 번호를 정확히 추출
CURRENT_APP_PORT=$(grep -A 1 'upstream current_app' "$NGINX_CONF" | tail -n 1 | grep -oP '127\.0\.0\.1:\K[0-9]+')


[[ -z "$CURRENT_APP_PORT" ]] && CURRENT_APP_PORT=$BLUE_PORT

echo "▶ 현재 포트: $CURRENT_APP_PORT"

# ────────── 2. 새 서비스/포트 결정 & 충돌 방지 정리 ────────────────────────
if [[ "$CURRENT_APP_PORT" == "$BLUE_PORT" ]]; then
  NEW_APP_PORT=$GREEN_PORT; NEW_APP_SERVICE=$GREEN_SERVICE; OLD_APP_SERVICE=$BLUE_SERVICE
else
  NEW_APP_PORT=$BLUE_PORT;  NEW_APP_SERVICE=$BLUE_SERVICE;  OLD_APP_SERVICE=$GREEN_SERVICE
fi
echo "[2/5] 새 포트: $NEW_APP_PORT / 새 서비스: $NEW_APP_SERVICE"

# 이미 떠 있는 동명 컨테이너가 있으면 삭제
$COMPOSE -f "$COMPOSE_FILE" rm -f "$NEW_APP_SERVICE" || true

echo "────────── 앱 환경 변수 export ──────────"
export IMAGE_URI="${FULL_IMAGE_NAME}" # Docker Compose가 사용할 이미지 URI
echo "▶ 앱 환경 변수 export 완료."

# 새 컨테이너 기동 (+ 실패 시 즉시 중단)
echo "▶ 새 앱 컨테이너 '${NEW_APP_SERVICE}' 기동 시작..."
# 이제 IMAGE_URI와 다른 환경 변수들은 이미 export되어 있으므로 별도 앞에 붙일 필요 없음
$COMPOSE -f "$COMPOSE_FILE" up -d "$NEW_APP_SERVICE" \
  || { echo "새 앱 기동 실패"; exit 1; }
echo "▶ 새 앱 컨테이너 기동 완료."

# ────────── 3. 헬스체크 & 실패 시 롤백 ────────────────────────────────────
echo "[3/5] 헬스체크 대기…"
HEALTH_URL="http://localhost:${NEW_APP_PORT}/actuator/health"
MAX=20; INTERVAL=10

for ((i=1; i<=MAX; i++)); do
  code=$(curl -s -o /dev/null -w "%{http_code}" "$HEALTH_URL" || true)
  ok=$(curl -s "$HEALTH_URL" | grep -q '"UP"' && echo yes || echo no)
  [[ "$code" == 200 && "$ok" == yes ]] && { echo "✅ 헬스체크 통과 ($i/$MAX)"; break; }
  echo "⏳ 재시도 $i/$MAX (code $code)"; sleep $INTERVAL
  [[ $i -eq $MAX ]] && { echo "🚨 헬스체크 실패 → 롤백"; $COMPOSE -f "$COMPOSE_FILE" rm -f "$NEW_APP_SERVICE"; exit 1; }
done

# ────────── 4. Nginx 스위치 & 이전 서비스 종료 ────────────────────────────
echo "[4/5] Nginx 트래픽 전환"

sudo sed -E -i '/upstream current_app/,+1 s/127\.0\.0\.1:[0-9]+/127.0.0.1:'"$NEW_APP_PORT"'/' "$NGINX_CONF" \
  || { echo "❌ Nginx 설정 치환 실패"; exit 1; }
sudo nginx -t || { echo "❌ Nginx 설정 오류"; exit 1; }
sudo systemctl reload nginx || { echo "❌ Nginx reload 실패"; exit 1; }
echo "▶ Nginx가 $NEW_APP_SERVICE($NEW_APP_PORT) 로 전환됨"

$COMPOSE -f "$COMPOSE_FILE" stop "$OLD_APP_SERVICE" || true
$COMPOSE -f "$COMPOSE_FILE" rm  -f "$OLD_APP_SERVICE" || true

echo "🎉 배포 완료! Blue/Green 전환 성공."


# ────────── 5. 모니터링 스택 배포 및 기동 ────────────────────────────

echo "[5/5] 모니터링 스택 배포 및 기동"
# Grafana 비밀번호를 .env 파일에서 참조할 수 있도록 임시 환경 변수로 내보냅니다.
# deploy.yml의 envs: 섹션에서 GRAFANA_ADMIN_PASSWORD를 받아옵니다.
export GRAFANA_ADMIN_PASSWORD="${GRAFANA_ADMIN_PASSWORD}"

cd "$MONITORING_DIR" # monitoring 디렉토리로 이동

# 모니터링 스택 컨테이너 중지 및 삭제 (재배포 시 클린 시작)
$COMPOSE -f monitoring-compose.yml down || true

# 모니터링 스택 기동
$COMPOSE -f monitoring-compose.yml up -d \
  || { echo "❌ 모니터링 스택 기동 실패!"; exit 1; }

echo "✅ 모니터링 스택 배포 완료."