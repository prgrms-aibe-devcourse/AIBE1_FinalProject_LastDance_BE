# Stage 1: Build
FROM azul/zulu-openjdk:17-latest AS build
# 컨테이너 내 작업 디렉토리는 /app으로 유지 (일반적 관례)
WORKDIR /app

# 그래들 파일 복사 및 의존성 캐싱
# Dockerfile이 docker 폴더 내에 있으므로, 상위 폴더의 파일을 참조해야 합니다.
COPY ../gradlew .
COPY ../gradle ./gradle
COPY ../build.gradle ./
COPY ../settings.gradle ./
RUN chmod +x ./gradlew

# 소스 코드 복사 및 빌드
COPY ../src src
# src 폴더도 상위에서 복사
RUN ./gradlew build -x test

# Stage 2: Runtime
FROM azul/zulu-openjdk:17-latest
WORKDIR /app

# 빌드 스테이지에서 JAR 파일만 복사
COPY --from=build /app/build/libs/*.jar app.jar

# Nginx가 프록시할 포트들을 명시. 실제 포트는 docker-compose에서 할당
EXPOSE 8080
EXPOSE 8081

# Spring 프로필을 'prod'로 설정
ENV SPRING_PROFILES_ACTIVE=prod

# Spring Boot 서버 포트를 환경 변수로 주입받을 준비.
# 이 값은 docker-compose.yml에서 주입됩니다.
ENTRYPOINT ["java", "-jar", "app.jar", "--server.port=${SERVER_PORT}"]