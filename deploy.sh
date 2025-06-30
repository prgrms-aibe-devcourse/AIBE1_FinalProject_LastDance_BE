#!/usr/bin/env bash
###############################################################################
# Blue/Green 배포 스크립트 (EC2 내부 실행, 단일 인스턴스용)
###############################################################################
set -eEuo pipefail                # -E: ERR 트랩 포함

############################ 0. 공통 변수 #####################################
PROJECT_NAME="lastdance-app"
APP_DIR="/home/ubuntu/${PROJECT_NAME}"
DOCKER_APP_DIR="${APP_DIR}/docker"
MONITORING_DIR="${APP_DIR}/monitoring"

COMPOSE_FILE="${DOCKER_APP_DIR}/docker-compose.yml"
NGINX_CONF="/etc/nginx/sites-available/lastdance-app.conf"

BLUE_PORT=8080;  GREEN_PORT=8081
BLUE_SERVICE="blue-app";  GREEN_SERVICE="green-app"

COMPOSE="docker-compose"
command -v docker compose &>/dev/null && COMPOSE="docker compose"

############################ 0-1. 고아 컨테이너 처리 ##########################
DEPLOY_OK=false
cleanup() {
  if [[ "$DEPLOY_OK" == false ]]; then
    echo "[CLEANUP] 롤백 – $NEW_APP_SERVICE 제거"
    $COMPOSE -f "$COMPOSE_FILE" rm -fs "$NEW_APP_SERVICE" 2>/dev/null || true
  fi
}
trap cleanup EXIT ERR INT        # 어떤 종료라도 cleanup 실행

############################ 0-2. 이미지 풀링 ################################
echo "── ECR 로그인 & 최신 이미지 Pull ──"
FULL_IMAGE_NAME="${ECR_URI}/${ECR_REPO}:${IMAGE_TAG}"

aws ecr get-login-password --region "$AWS_REGION" | \
  docker login --username AWS --password-stdin "$ECR_URI"
docker pull "$FULL_IMAGE_NAME"

############################ 0-3. 네트워크 ###################################
docker network create app-monitor-network || true

############################ 1. 현재 활성 포트 ################################
CURRENT_APP_PORT=$(grep -A1 'upstream current_app' "$NGINX_CONF" \
                    | tail -n1 | grep -oP '127\.0\.0\.1:\K[0-9]+')
[[ -z "$CURRENT_APP_PORT" ]] && CURRENT_APP_PORT=$BLUE_PORT
if [[ "$CURRENT_APP_PORT" == "$BLUE_PORT" ]]; then
  NEW_APP_PORT=$GREEN_PORT; NEW_APP_SERVICE=$GREEN_SERVICE; OLD_APP_SERVICE=$BLUE_SERVICE
else
  NEW_APP_PORT=$BLUE_PORT;  NEW_APP_SERVICE=$BLUE_SERVICE;  OLD_APP_SERVICE=$GREEN_SERVICE
fi
echo "새 서비스: $NEW_APP_SERVICE  (포트 $NEW_APP_PORT)"

############################ 2. 새 컨테이너 기동 ##############################
export IMAGE_URI="$FULL_IMAGE_NAME"          # docker-compose.yml 에서 사용
$COMPOSE -f "$COMPOSE_FILE" rm -fs "$NEW_APP_SERVICE" || true

echo "컨테이너 기동…"
# (compose v2.21+ 사용 시 주석 해제해 헬스체크 통합)
# $COMPOSE -f "$COMPOSE_FILE" up -d --wait -t 180 "$NEW_APP_SERVICE"
$COMPOSE -f "$COMPOSE_FILE" up -d "$NEW_APP_SERVICE"

############################ 3. 헬스체크 (curl 방식) ###########################
echo "헬스체크 대기…"
HEALTH_URL="http://localhost:${NEW_APP_PORT}/actuator/health"
MAX=20; INTERVAL=10

for ((i=1;i<=MAX;i++)); do
  if curl -sf "$HEALTH_URL" | grep -q '"UP"'; then
      echo "✅ 헬스 통과 ($i/$MAX)"
      break
  fi
  [[ $i -eq $MAX ]] && { echo "🚨 헬스 실패"; exit 1; }
  echo "⏳ 재시도 $i/$MAX"; sleep $INTERVAL
done

############################ 4. Nginx 스위치 ##################################
sudo sed -E -i '/upstream current_app/,+1 s/127\.0\.0\.1:[0-9]+/127.0.0.1:'"$NEW_APP_PORT"'/' \
  "$NGINX_CONF"
sudo nginx -t
sudo systemctl reload nginx
echo "Nginx가 $NEW_APP_SERVICE 로 전환됨"

$COMPOSE -f "$COMPOSE_FILE" stop "$OLD_APP_SERVICE" || true
$COMPOSE -f "$COMPOSE_FILE" rm  -fs "$OLD_APP_SERVICE" || true

DEPLOY_OK=true    # ★ 여기서 성공 플래그 ON

echo "🎉 Blue/Green 전환 완료"

############################ 5. 모니터링 스택 재배포 ##########################
cd "$MONITORING_DIR"

echo "→ Rendering Alertmanager config with SLACK_WEBHOOK_URL"
envsubst < prometheus/alertmanager.yml \
  > prometheus/alertmanager.rendered.yml

# 치환된 파일로 스택 재기동
$COMPOSE -f monitoring-compose.yml down || true
$COMPOSE -f monitoring-compose.yml up -d

echo "✅ 모니터링 스택 기동 완료"
