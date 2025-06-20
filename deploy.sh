#!/bin/bash

# 배포 스크립트: EC2 인스턴스에서 실행될 실제 배포 로직

# 환경 변수 (GitHub Actions에서 주입됨)
PROJECT_NAME="lastdance-app"
ECR_REPOSITORY_URI="$1" # GitHub Actions에서 첫 번째 인자로 전달받음
AWS_REGION="$2"          # GitHub Actions에서 두 번째 인자로 전달받음

# ----------------------------------------------------
# 1. ECR 로그인 및 최신 Docker 이미지 가져오기
# ----------------------------------------------------
echo "[1/5] ECR 로그인 및 최신 Docker 이미지 가져오기..."
# AWS CLI를 사용하여 ECR에 로그인합니다.
aws ecr get-login-password --region ${AWS_REGION} | docker login --username AWS --password-stdin ${ECR_REPOSITORY_URI} || { echo "ECR 로그인 실패!"; exit 1; }

# 최신 이미지 태그를 정의합니다. (예: latest)
IMAGE_TAG="latest" # 또는 GitHub Actions run ID 등을 사용 가능
FULL_IMAGE_NAME="${ECR_REPOSITORY_URI}:${IMAGE_TAG}"

# 최신 Docker 이미지 pull
docker pull ${FULL_IMAGE_NAME} || { echo "Docker 이미지 풀 실패! 이미지가 존재하지 않거나 권한 문제."; exit 1; }

# ----------------------------------------------------
# 2. 현재 활성화된 앱 포트 확인 (Blue/Green)
# ----------------------------------------------------
echo "[2/5] 현재 활성화된 앱 포트 확인..."

# Nginx 설정 파일을 통해 현재 어느 앱이 활성화되어 있는지 확인
# Nginx 설정 파일에서 'current_app'이 가리키는 포트를 읽어옵니다.
CURRENT_APP_PORT=$(cat /etc/nginx/sites-available/lastdance-app.conf | grep -A 1 "upstream current_app" | grep "server 127.0.0.1" | awk -F: '{print $2}' | awk '{print $1}')

echo "현재 활성화된 앱 포트: ${CURRENT_APP_PORT}"

# ----------------------------------------------------
# 3. 배포할 새 앱의 포트 결정
# ----------------------------------------------------
echo "[3/5] 배포할 새 앱의 포트 결정..."

NEW_APP_PORT=""
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

# ----------------------------------------------------
# 4. 새로운 앱 배포 및 헬스 체크
# ----------------------------------------------------
echo "[4/5] 새로운 앱 (${NEW_APP_SERVICE}) 배포 및 헬스 체크 시작..."

# Docker Compose를 사용하여 새로운 앱 서비스만 실행
# build는 이미지가 pull되었으므로 생략 가능. 하지만 만일을 위해 포함.
docker-compose -f /home/ubuntu/${PROJECT_NAME}/docker/docker-compose.yml up -d --build ${NEW_APP_SERVICE} || { echo "새 앱 서비스 구동 실패!"; exit 1; }

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
        # 배포 실패 시 이전 앱으로 롤백하거나 수동 개입 필요
        exit 1
    fi
done

# ----------------------------------------------------
# 5. Nginx 포트 전환 및 이전 앱 종료
# ----------------------------------------------------
echo "[5/5] Nginx 포트 전환 및 이전 앱 종료..."

# Nginx 설정 파일 수정 (현재 active upstream 변경)
# sed 명령으로 current_app이 가리키는 포트를 새로운 앱 포트로 변경
sudo sed -i "s|server 127.0.0.1:${CURRENT_APP_PORT}|server 127.0.0.1:${NEW_APP_PORT}|g" /etc/nginx/sites-available/lastdance-app.conf || { echo "Nginx 설정 파일 수정 실패!"; exit 1; }

# Nginx 설정 유효성 검사
sudo nginx -t || { echo "Nginx 설정 유효성 검사 실패! Nginx가 재시작되지 않습니다."; exit 1; }

# Nginx 재로드 (서비스 중단 없이 설정만 재적용)
sudo systemctl reload nginx || { echo "Nginx 재로드 실패!"; exit 1; }

echo "Nginx가 새로운 앱 (${NEW_APP_SERVICE})으로 트래픽을 전환했습니다."

# 이전 앱 서비스 종료
echo "이전 앱 (${OLD_APP_SERVICE}) 서비스 종료..."
docker-compose -f /home/ubuntu/${PROJECT_NAME}/docker/docker-compose.yml down ${OLD_APP_SERVICE} || { echo "이전 앱 종료 실패 (경고)!"; }

echo "배포 완료! 🎉"