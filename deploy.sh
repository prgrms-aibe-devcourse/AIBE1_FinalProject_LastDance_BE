#!/usr/bin/env bash
###############################################################################
# Blue/Green 배포 스크립트 (EC2 내부 실행)
#   • 현재 트래픽 포트 확인 → 새 컨테이너 기동 → 헬스체크 성공 시 Nginx 스위치
#   • 실패 시 자동 롤백 및 오류 코드 반환
###############################################################################
set -euo pipefail

# ────────────────────────────── 0. 공통 변수 ────────────────────────────────
PROJECT_NAME="lastdance-app"
NGINX_CONF="/etc/nginx/sites-available/lastdance-app.conf"
BLUE_PORT=8080
GREEN_PORT=8081
BLUE_SERVICE="blue_app"
GREEN_SERVICE="green_app"

# docker-compose v1, docker compose v2 모두 호환되게 커맨드 자동 감지
COMPOSE="docker-compose"
if command -v docker compose &>/dev/null; then
  COMPOSE="docker compose"
fi

# ─────────────────── 1. 현재 활성 포트(blue or green) 파악 ──────────────────
echo "[1/4] 현재 활성화된 앱 포트 확인…"

CURRENT_APP_PORT=$(
  awk '/upstream current_app/{getline; if ($0 ~ /127\.0\.0\.1/) {split($0,a,":"); split(a[2],b,";"); print b[1]}}' \
    "${NGINX_CONF}" || true
)

if [[ -z "${CURRENT_APP_PORT}" ]]; then
  echo "초기 배포 또는 설정값 없음 → 기본 ${BLUE_PORT} 사용"
  CURRENT_APP_PORT=$BLUE_PORT
fi
echo "▶ 현재 포트: ${CURRENT_APP_PORT}"

# ─────────────────── 2. 새 서비스/포트 결정 & 사전 정리 ────────────────────
echo "[2/4] 새 앱 기동 준비…"

if [[ "${CURRENT_APP_PORT}" == "${BLUE_PORT}" ]]; then
  NEW_APP_PORT=${GREEN_PORT}
  NEW_APP_SERVICE=${GREEN_SERVICE}
  OLD_APP_SERVICE=${BLUE_SERVICE}
else
  NEW_APP_PORT=${BLUE_PORT}
  NEW_APP_SERVICE=${BLUE_SERVICE}
  OLD_APP_SERVICE=${GREEN_SERVICE}
fi

echo "▶ 새 포트: ${NEW_APP_PORT} / 새 서비스: ${NEW_APP_SERVICE}"

# 동일 이름 컨테이너가 남아 있으면 충돌하므로 미리 정리
${COMPOSE} rm -f ${NEW_APP_SERVICE} || true

# 새 컨테이너 기동 (이미지는 GitHub Actions 단계에서 pull 완료/최신화)
${COMPOSE} -f /home/ubuntu/${PROJECT_NAME}/docker/docker-compose.yml \
  up -d ${NEW_APP_SERVICE} || { echo "❌ 새 앱 기동 실패"; exit 1; }

# ───────────────────── 3. 헬스체크 & 롤백 로직 ─────────────────────
echo "[3/4] 헬스체크 대기…"
HEALTH_URL="http://localhost:${NEW_APP_PORT}/actuator/health"
MAX_TRIES=20
INTERVAL=10

for ((i=1; i<=MAX_TRIES; i++)); do
  STATUS_CODE=$(curl -s -o /dev/null -w "%{http_code}" "${HEALTH_URL}" || true)
  BODY_OK=$(curl -s "${HEALTH_URL}" | grep -q '"UP"' && echo "yes" || echo "no")

  if [[ "${STATUS_CODE}" == "200" && "${BODY_OK}" == "yes" ]]; then
    echo "✅ 헬스체크 통과 (${i}/${MAX_TRIES})"
    break
  fi

  echo "⏳ 헬스체크 재시도 ${i}/${MAX_TRIES} (응답 ${STATUS_CODE})"
  sleep "${INTERVAL}"

  if [[ $i -eq ${MAX_TRIES} ]]; then
    echo "🚨 새 앱 헬스체크 실패 → 롤백 수행"
    ${COMPOSE} rm -f ${NEW_APP_SERVICE} || true
    exit 1
  fi
done

# ───────────────────── 4. Nginx 스위치 & 정리 ─────────────────────
echo "[4/4] Nginx 트래픽 전환 → 이전 컨테이너 종료"

# upstream current_app 블록 내부 한 줄만 치환 (포트만 변경)
sudo sed -E -i \
  '/upstream current_app/,+1 s/127\.0\.0\.1:[0-9]+/127.0.0.1:'"${NEW_APP_PORT}"'/' \
  "${NGINX_CONF}" \
  || { echo "❌ Nginx 설정 치환 실패"; exit 1; }

sudo nginx -t || { echo "❌ Nginx 설정 오류"; exit 1; }
sudo systemctl reload nginx || { echo "❌ Nginx reload 실패"; exit 1; }
echo "▶ Nginx가 ${NEW_APP_SERVICE}(${NEW_APP_PORT})로 전환됨"

# 이전 서비스 종료
${COMPOSE} stop ${OLD_APP_SERVICE} || true
${COMPOSE} rm -f  ${OLD_APP_SERVICE} || true

echo "🎉 배포 완료! Blue/Green 전환 성공."
