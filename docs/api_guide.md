# API 키 관리 가이드

API 키가 추가될 때마다 아래 절차를 따라주세요.

---

## 1. `docker-compose.yml`에 환경 변수명 입력

**프로젝트 루트에 있는 `doker/docker-compose.yml` 파일에서 서비스 환경 변수 섹션에 새 키 이름을 추가합니다.**

> ### 주의할 점
> 
> **green, blue 둘 다 값을 넣어주세요**

```yaml
# docker-compose.yml
# ... 윗 내용 생략
blue-app: # <- Blue 확인
  # ... 생략
  ####################################
  environment: 
    - SPRING_PROFILES_ACTIVE=prod
    - SERVER_PORT=8080     #  앱 내부 포트 지정
    - JAVA_TOOL_OPTIONS=-XX:MaxRAMPercentage=75   # 힙≈380 MB
    #  향후 새로운 환경 변수 추가 시 여기에 이름만 추가해야 합니다.
    ########## 여기부터 값 넣기 ###########
    - DB_URL
    - DB_USERNAME
    - DB_PASSWORD
    - API_KEY  # ← 새로 추가한 API 키 이름
      # - OTHER_KEY  # 필요 시 추가 환경 변수
      
  # ... 중간내용 생략
  ################################################
  green-app: # < Green 확인
    # ... 생략
    environment: 
      - SPRING_PROFILES_ACTIVE=prod
      - SERVER_PORT=8081     #
      - JAVA_TOOL_OPTIONS=-XX:MaxRAMPercentage=75   # 힙≈380 MB
      #  향후 새로운 환경 변수 추가 시 여기에 이름만 추가해야 합니다.
      ###############여기에도 빼먹지말고 값 넣기##################
      # 
      # ...
      - REDIS_HOST
      - REDIS_PORT
      - REDIS_PASSWORD
      - AWS_S3_BUCKET_NAME
      - AWS_ACCESS_KEY_ID
      - AWS_SECRET_ACCESS_KEY
      - API_KEY  # ← 새로 추가한 API 키 이름
      # - OTHER_KEY  # 필요 시 추가 환경 변수
```
---
## 2. `.github/workflows/deploy.yml`에 환경 변수명 입력
GitHub Actions 배포 워크플로우(`.github/workflows/deploy.yml`)에 env 항목으로 키 이름과 값을 추가합니다.
1. `env` 에 `이름`과 `${{ secrets.값 }}` 입력
2. `envs` 에 `이름`만 입력

```yaml
# ... 윗 내용 생략, 8-B 로 이동할 것

# 8-B. 원격 배포 (SSH)
- name: Deploy to EC2
  uses: appleboy/ssh-action@v1.0.0
  env: # ← 러너 쪽에 먼저 값 정의
    ECR_URI: ${{ steps.login-ecr.outputs.registry }}
    ECR_REPO: ${{ env.ECR_REPOSITORY }}
    AWS_REGION: ${{ env.AWS_REGION }}
    IMAGE_TAG: ${{ github.sha }}
    ###################이곳에 추가할 키이름, 값 입력#############################
    AWS_ACCESS_KEY_ID: ${{ secrets.AWS_ACCESS_KEY_ID }}
    # ... 중간 생략
    NAVER_CLIENT_SECRET: ${{ secrets.NAVER_CLIENT_SECRET }}
    GRAFANA_ADMIN_PASSWORD: ${{ secrets.GRAFANA_ADMIN_PASSWORD }}
    GOOGLE_GEMINI_KEY: ${{ secrets.GOOGLE_GEMINI_KEY }}
    REDIS_HOST: ${{ secrets.REDIS_HOST }}
    REDIS_PORT: ${{ secrets.REDIS_PORT }}
    REDIS_PASSWORD: ${{ secrets.REDIS_PASSWORD }}
    # 예시 
    # REDIS_PASSWORD1: ${{ secrets.REDIS_PASSWORD1 }}
    # REDIS_PASSWORD2: ${{ secrets.REDIS_PASSWORD2 }}
    # REDIS_PASSWORD3: ${{ secrets.REDIS_PASSWORD3 }}
    ###################################################################
  with:
    host: ${{ secrets.EC2_HOST }}
    username: ${{ secrets.EC2_USERNAME }}
    key: ${{ secrets.SSH_PRIVATE_KEY }}
    script_stop: true

    # ① KEY 목록만 나열 (line 모드·envs_format 생략)
    
    # 콤마를 사용하여 키 이름만 추가할 것!
    ################이곳에 추가할 키 이름만 입력##############    
    envs: >
      ECR_URI,ECR_REPO,AWS_REGION,IMAGE_TAG,
      DB_URL,DB_USERNAME,DB_PASSWORD,JWT_SECRET_KEY,
      GOOGLE_CLIENT_ID,GOOGLE_CLIENT_SECRET,
      KAKAO_CLIENT_ID,KAKAO_CLIENT_SECRET,
      NAVER_CLIENT_ID,NAVER_CLIENT_SECRET,
      GRAFANA_ADMIN_PASSWORD,
      GOOGLE_GEMINI_KEY,
      REDIS_HOST,REDIS_PORT,REDIS_PASSWORD,
      AWS_S3_BUCKET_NAME,AWS_ACCESS_KEY_ID,AWS_SECRET_ACCESS_KEY
#########################################################
```
> env: API_KEY: ${{ secrets.API_KEY }} 부분의 API_KEY 역시 Secrets에 등록한 이름과 같아야 합니다.
---
## 3. GitHub Actions Secrets에 키 등록
> GitHub 리포지토리 페이지 ▶ Settings
>
> 왼쪽 메뉴 ▶ Secrets and variables ▶ Actions
> 
> New repository secret 클릭
>
> Name: API_KEY
>
> Value: 실제 발급된 API 키 값
> 
> Add secret 클릭

추가 API 키가 생길 때마다 위 Name(API_KEY)과 Value(키 값)를 그대로 새 Secret으로 등록해주세요.

## 4. Application-dev,prod 에 키 등록
> dev 뿐 아니라 prod에도 등록을 잊지 마세요

---
# 요약
### 1. `docker-compose.yml`:green, blue에 각각 env 변수명 추가

### 2. `.github/workflows/deploy.yml`: 
- `env`: API_KEY: ${{ secrets.API_KEY }} 추가

- `envs` : API_KEY 이름만 추가 `,` 주의 


### 3. `GitHub Secrets`: Settings ▶ Actions ▶ Secrets ▶ API_KEY 등록

### 4. `application-dev` , `appication-prod` 에 키 추가

---
