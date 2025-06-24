#!/usr/bin/env bash
###############################################################################
# Blue/Green 배포 스크립트 (EC2 내부 실행)
###############################################################################
set -euo pipefail

# ────────── 0. 공통 변수 ────────────────────────────────────────────────────
PROJECT_NAME="lastdance-app"
COMPOSE_FILE="/home/ubuntu/${PROJECT_NAME}/docker/docker-compose.yml"   # ▶ ① 반복 경로 변수화
NGINX_CONF="/etc/nginx/sites-available/lastdance-app.conf"

BLUE_PORT=8080
GREEN_PORT=8081
BLUE_SERVICE="blue_app"
GREEN_SERVICE="green_app"

# docker-compose v1, v2 자동 감지
COMPOSE="docker-compose"
command -v docker compose &>/dev/null && COMPOSE="docker compose"

# ────────── 1. 현재 활성 포트 파악(숫자만) ────────────────────────────────
echo "[1/4] 현재 활성화된 앱 포트 확인…"

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
echo "[2/4] 새 포트: $NEW_APP_PORT / 새 서비스: $NEW_APP_SERVICE"

# 이미 떠 있는 동명 컨테이너가 있으면 삭제
$COMPOSE -f "$COMPOSE_FILE" rm -f "$NEW_APP_SERVICE" || true

# 새 컨테이너 기동 (+ 실패 시 즉시 중단)
IMAGE_URI="${IMAGE_URI:-latest}" \
  $COMPOSE -f "$COMPOSE_FILE" up -d "$NEW_APP_SERVICE" \
  || { echo "새 앱 기동 실패"; exit 1; }

# ────────── 3. 헬스체크 & 실패 시 롤백 ────────────────────────────────────
echo "[3/4] 헬스체크 대기…"
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
echo "[4/4] Nginx 트래픽 전환"

sudo sed -E -i '/upstream current_app/,+1 s/127\.0\.0\.1:[0-9]+/127.0.0.1:'"$NEW_APP_PORT"'/' "$NGINX_CONF" \
  || { echo "❌ Nginx 설정 치환 실패"; exit 1; }
sudo nginx -t || { echo "❌ Nginx 설정 오류"; exit 1; }
sudo systemctl reload nginx || { echo "❌ Nginx reload 실패"; exit 1; }
echo "▶ Nginx가 $NEW_APP_SERVICE($NEW_APP_PORT) 로 전환됨"

$COMPOSE -f "$COMPOSE_FILE" stop "$OLD_APP_SERVICE" || true
$COMPOSE rm -f "$COMPOSE_FILE" rm  -f "$OLD_APP_SERVICE" || true

echo "🎉 배포 완료! Blue/Green 전환 성공."
