#!/bin/bash

# 배포 스크립트: EC2 인스턴스에서 실행될 실제 배포 로직

# 환경 변수 (GitHub Actions의 script 블록에서 이미 처리됨)
PROJECT_NAME="lastdance-app" # 이 변수는 스크립트 내부에서 계속 사용 가능

# ECR 로그인 및 이미지 풀은 deploy.yml 스크립트에서 이미 완료되었음을 전제

# ----------------------------------------------------
# 1. 현재 활성화된 앱 포트 확인 (Blue/Green)
# ----------------------------------------------------
echo "[1/3] 현재 활성화된 앱 포트 확인..."

# Nginx 설정 파일을 통해 현재 어느 앱이 활성화되어 있는지 확인
# Nginx 설정 파일에서 'current_app'이 가리키는 포트를 읽어옵니다.
CURRENT_APP_PORT=$(cat /etc/nginx/sites-available/lastdance-app.conf | grep -A 1 "upstream current_app" | grep "server 127.0.0.1" | awk -F: '{print $2}' | awk '{print $1}')

# 초기 배포 시 CURRENT_APP_PORT가 비어있을 수 있음
if [ -z "$CURRENT_APP_PORT" ]; then
    echo "초기 배포 또는 현재 활성화된 앱이 없습니다. 8080 포트를 기본으로 설정합니다."
    CURRENT_APP_PORT="8080" # 기본값 설정
fi

echo "현재 활성화된 앱 포트: ${CURRENT_APP_PORT}"

# ----------------------------------------------------
# 2. 배포할 새 앱의 포트 결정 및 실행
# ----------------------------------------------------
echo "[2/3] 배포할 새 앱의 포트 결정 및 실행 시작..."

NEW_APP_PORT=""
NEW_APP_SERVICE=""
OLD_APP_SERVICE=""

if [ "$CURRENT_APP_PORT" = "8080" ]; then
    NEW_APP_PORT="8081"
    NEW_APP_SERVICE="green_app"
    OLD_APP_SERVICE="blue_app"
else
    NEW_APP_PORT="8080"
    NEW_APP_SERVICE="blue_app"
    OLD_APP_SERVICE="green_app"
fi

echo "새로운 앱 포트: ${NEW_APP_PORT}"
echo "배포할 서비스: ${NEW_APP_SERVICE}"

# Docker Compose를 사용하여 새로운 앱 서비스만 실행 (이미 풀된 이미지 사용)
# -f 경로를 정확하게 지정합니다.
docker-compose -f /home/ubuntu/${PROJECT_NAME}/docker/docker-compose.yml up -d ${NEW_APP_SERVICE} || { echo "새 앱 서비스 구동 실패!"; exit 1; }

echo "새로운 앱 (${NEW_APP_SERVICE})이 시작 중입니다. 헬스 체크를 기다립니다..."

# 헬스 체크 루프
HEALTH_CHECK_URL="http://localhost:${NEW_APP_PORT}/actuator/health"
MAX_RETRIES=20
RETRY_INTERVAL=10 # 초

for i in $(seq 1 $MAX_RETRIES); do
    echo "헬스 체크 시도 ${i}/${MAX_RETRIES}..."
    HEALTH_STATUS=$(curl -s -o /dev/null -w "%{http_code}" ${HEALTH_CHECK_URL})

    if [ "$HEALTH_STATUS" = "200" ]; then
        echo "새로운 앱 (${NEW_APP_SERVICE}) 헬스 체크 성공! (${HEALTH_CHECK_URL} 응답 코드: ${HEALTH_STATUS})"
        break
    else
        echo "새로운 앱 (${NEW_APP_SERVICE}) 헬스 체크 실패! (${HEALTH_CHECK_URL} 응답 코드: ${HEALTH_STATUS}). ${RETRY_INTERVAL}초 후 재시도..."
        sleep ${RETRY_INTERVAL}
    fi

    if [ "$i" -eq "$MAX_RETRIES" ]; then
        echo "🚨 헬스 체크 실패: 새로운 앱이 정상적으로 시작되지 않았습니다. 롤백을 고려하세요. 🚨"
        exit 1
    fi
done

# ----------------------------------------------------
# 3. Nginx 포트 전환 및 이전 앱 종료
# ----------------------------------------------------
echo "[3/3] Nginx 포트 전환 및 이전 앱 종료..."

# Nginx 설정 파일 수정 (현재 active upstream 변경)
sudo sed -i "s|server 127.0.0.1:${CURRENT_APP_PORT}|server 127.0.0.1:${NEW_APP_PORT}|g" /etc/nginx/sites-available/lastdance-app.conf || { echo "Nginx 설정 파일 수정 실패!"; exit 1; }

# Nginx 설정 유효성 검사
sudo nginx -t || { echo "Nginx 설정 유효성 검사 실패! Nginx가 재시작되지 않습니다."; exit 1; }

# Nginx 재로드 (서비스 중단 없이 설정만 재적용)
sudo systemctl reload nginx || { echo "Nginx 재로드 실패!"; exit 1; }

echo "Nginx가 새로운 앱 (${NEW_APP_SERVICE})으로 트래픽을 전환했습니다."

# 이전 앱 서비스 종료 (v1/v2 모두 호환)
echo "이전 앱 (${OLD_APP_SERVICE}) 서비스 종료..."
docker-compose stop ${OLD_APP_SERVICE} || true
docker-compose rm -f ${OLD_APP_SERVICE} || true

echo "배포 완료! 🎉"