
# 우리.zip - 생활 관리 플랫폼 (Backend)

## 팀 소개


| 조다미 | 반준영 | 한정호 | 진소희 | 조준호 |
|:------:|:------:|:------:|:------:|:------:|
| <img src="https://avatars.githubusercontent.com/dochmai382" width="100"/> | <img src="https://avatars.githubusercontent.com/aibeban" width="100"/> | <img src="https://avatars.githubusercontent.com/hanjungho" width="100"/> | <img src="https://avatars.githubusercontent.com/soheeGit" width="100"/> | <img src="https://avatars.githubusercontent.com/lSNOTNULL" width="100"/> |
| [@dochmai382](https://github.com/dochmai382) | [@aibeban](https://github.com/aibeban) | [@hanjungho](https://github.com/hanjungho) | [@soheeGit](https://github.com/soheeGit) | [@lSNOTNULL](https://github.com/lSNOTNULL) |


## 프로젝트 개요
**우리.zip**은 하우스메이트와 함께하는 스마트한 공동생활 관리 플랫폼입니다.  
일상 관리, 소비 분석, 집안일 분담 등을 하나의 서비스로 통합하여 쾌적하고 공정한 공동 생활 환경을 조성하는 웹 기반 서비스입니다.

## 시스템 아키텍쳐
<img width="800" alt="Image" src="https://github.com/user-attachments/assets/cdc751b8-ca9f-4ca5-ad66-37c4b95210ce" />

## 기술스택
| 구분 | 기술                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     |
| :--- |:-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Backend** | ![Java](https://img.shields.io/badge/Java%2017-007396?style=for-the-badge&logo=openjdk&logoColor=white) ![Spring Boot](https://img.shields.io/badge/Spring%20Boot%203.5.0-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white) ![Spring Security](https://img.shields.io/badge/Spring%20Security-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white) ![Spring Data JPA](https://img.shields.io/badge/Spring%20Data%20JPA-6DB33F?style=for-the-badge&logo=spring&logoColor=white) ![PostgreSQL](https://img.shields.io/badge/PostgreSQL-4169E1?style=for-the-badge&logo=postgresql&logoColor=white) ![Redis](https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=redis&logoColor=white)                                                                              |
| **외부 서비스** | ![AWS S3](https://img.shields.io/badge/AWS%20S3-569A31?style=for-the-badge&logo=amazon-s3&logoColor=white) ![Google Gemini](https://img.shields.io/badge/Google%20Gemini-4285F4?style=for-the-badge&logo=google-gemini&logoColor=white) ![OAuth2](https://img.shields.io/badge/OAuth2-2496ED?style=for-the-badge&logo=oauth&logoColor=white) ![청년정책 API](https://img.shields.io/badge/청년정책%20API-0052CC?style=for-the-badge)                                                                                                                                                                                                                                                              |
| **배포** | ![AWS EC2](https://img.shields.io/badge/AWS%20EC2-FF9900?style=for-the-badge&logo=aws-ec2&logoColor=white) ![AWS ECR](https://img.shields.io/badge/AWS%20ECR-232F3E?style=for-the-badge&logo=aws-ecr&logoColor=white) ![AWS RDS](https://img.shields.io/badge/AWS%20RDS-527FFF?style=for-the-badge&logo=aws-rds&logoColor=white) ![AWS Route 53](https://img.shields.io/badge/AWS%20Route%2053-FF9900?style=for-the-badge&logo=aws-route-53&logoColor=white) ![AWS VPC](https://img.shields.io/badge/AWS%20VPC-FF9900?style=for-the-badge&logo=aws-vpc&logoColor=white) ![Nginx](https://img.shields.io/badge/Nginx-009639?style=for-the-badge&logo=nginx&logoColor=white) ![Let's Encrypt](https://img.shields.io/badge/Let's%20Encrypt-003A70?style=for-the-badge&logo=lets-encrypt&logoColor=white) |
| **모니터링**| ![Prometheus](https://img.shields.io/badge/Prometheus-E6522C?style=for-the-badge&logo=prometheus&logoColor=white) ![Grafana](https://img.shields.io/badge/Grafana-F46800?style=for-the-badge&logo=grafana&logoColor=white) ![Loki](https://img.shields.io/badge/Loki-F2B704?style=for-the-badge&logo=loki&logoColor=black) ![Promtail](https://img.shields.io/badge/Promtail-575757?style=for-the-badge&logo=grafana&logoColor=white)                                                                                                                                                                                                                                                                                                                                                                  |

## 주요 기능

| 도메인       | 주요 기능                                                               |
|--------------|------------------------------------------------------------------------|
| 사용자 관리  | 소셜 로그인(OAuth2), JWT 인증/인가, 프로필 이미지 업로드, 예산 관리     |
| 가계부 관리  | 수입/지출 내역 관리, 카테고리별 분류, 예산 대비 지출 분석               |
| 그룹 관리    | 그룹 생성 및 참여, 역할(방장/부방장/멤버) 관리, 가입 신청/승인          |
| 게임 기능    | 룰렛 게임, 사다리 타기, 야찌                                            |
| 체크리스트   | 할 일 관리, 캘린더 연동                                                |
| 알림 시스템  | 웹 푸시 알림, 실시간 알림(SSE), 이메일 알림                            |
| AI 기능      | Google Gemini AI 연동, 지출 패턴 분석 및 조언                           |
| 청년정책     | 청년정책 정보 조회, 맞춤형 정책 추천                                    |



## 실행 방법

### 1. 개발 환경 설정
```bash
git clone [repository-url]
cd AIBE1_FinalProject_LastDance_BE
```

**2. 환경 변수 설정**
`application-dev.yml`에 다음 설정 추가
```yaml
spring:
  profiles:
    active: dev
  datasource:
    url: jdbc:postgresql://localhost:5432/woorizip
    username: your_username
    password: your_password
```

**3. 빌드 및 실행**
```bash
./gradlew build      # Gradle 빌드
./gradlew bootRun    # 애플리케이션 실행
```

### Docker로 실행
```bash
docker build -f docker/Dockerfile -t woori-zip-be .
cd docker
docker-compose up -d
```


## 환경 변수 예시
```env
# 데이터베이스
DB_URL=jdbc:postgresql://localhost:5432/woorizip
DB_USERNAME=your_username
DB_PASSWORD=your_password

# JWT
JWT_SECRET_KEY=your_jwt_secret

# OAuth2
GOOGLE_CLIENT_ID=your_google_client_id
GOOGLE_CLIENT_SECRET=your_google_client_secret
KAKAO_CLIENT_ID=your_kakao_client_id
KAKAO_CLIENT_SECRET=your_kakao_client_secret
NAVER_CLIENT_ID=your_naver_client_id
NAVER_CLIENT_SECRET=your_naver_client_secret

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=your_redis_password

# AWS S3
AWS_S3_BUCKET_NAME=your_bucket_name
AWS_ACCESS_KEY_ID=your_access_key
AWS_SECRET_ACCESS_KEY=your_secret_key

# Google Gemini AI
GOOGLE_GEMINI_KEY=your_gemini_api_key

# 웹 푸시
VAPID_PUBLIC_KEY=your_vapid_public_key
VAPID_PRIVATE_KEY=your_vapid_private_key
VAPID_SUBJECT=mailto:your_email@example.com

# 청년정책 API
YOUTH_API_KEY=your_youth_policy_api_key
```

## 모니터링 및 헬스체크
-   **/actuator/health** : 애플리케이션 상태 확인
-   **/actuator/prometheus** : Prometheus 메트릭 수집
-   **AOP 기반 요청/응답 로깅**
-   **성능 측정 및 분석**

## 보안
-   **JWT 토큰 기반 인증/인가**
-   **Redis**를 통한 토큰 관리
-   **Rate Limiting** (요청 제한)
-   **CORS** 및 **XSS 방지**

## API 문서
-   **Swagger UI**  
    [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

## 배포
### Blue-Green 배포
-   GitHub Actions 자동 배포
-   Docker 컨테이너 기반 무중단 배포
-   헬스체크 기반 배포 검증

### CI/CD 파이프라인
1.  코드 푸시
2.  GitHub Actions 트리거
3.  빌드 및 테스트
4.  Docker 이미지 생성
5.  ECR 업로드
6.  EC2 배포


## API 키 관리
새로운 API 키 추가 시 다음 파일들을 수정해야 합니다.
1.  `docker/docker-compose.yml` - 환경 변수 추가
2.  `.github/workflows/deploy.yml` - GitHub Actions 환경 변수 추가
3.  **GitHub Secrets** - 실제 키 값 등록
4.  `application-dev.yml`, `application-prod.yml` - 설정 추가
> 자세한 내용은 `docs/api_guide.md` 파일을 참고하세요.

## 기여하기
1.  프로젝트 Fork
2.  기능 브랜치 생성  
    `git checkout -b feature/AmazingFeature`
3.  변경사항 커밋  
    `git commit -m 'Add some AmazingFeature'`
4.  브랜치 푸시  
    `git push origin feature/AmazingFeature`
5.  Pull Request 생성

## 라이선스
이 프로젝트는 **MIT 라이선스**를 따릅니다.

## 개발팀
-   Backend 개발: **LastDance Team**
-   프로젝트 기간: 2025년 6월 10일 ~ 2025년 7월 17일
    
