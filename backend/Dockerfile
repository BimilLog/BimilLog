# 1. 빌드 단계 (Build Stage)
FROM openjdk:21-jdk-slim AS builder
WORKDIR /app

# Gradle 파일 먼저 복사 (의존성 캐싱 활용)
COPY build.gradle settings.gradle ./
COPY gradle ./gradle
COPY gradlew ./
RUN chmod +x ./gradlew

# 의존성 다운로드 (소스코드 변경과 분리)
RUN ./gradlew dependencies --no-daemon

# 소스 코드 복사
COPY src ./src

# 애플리케이션 빌드 (테스트 제외)
RUN ./gradlew build -x test --no-daemon

# ---

# 2. 실행 단계 (Production Stage)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# 빌드 단계(builder)에서 생성된 JAR 파일만 복사
# 사용자께서 명시한 정확한 JAR 파일 경로를 사용합니다.
COPY --from=builder /app/build/libs/growfarm-1.0.0-RC1.jar app.jar

# 운영 환경 프로파일 활성화 (필요시 외부에서 변경 가능)
ENV SPRING_PROFILES_ACTIVE=prod

EXPOSE 8080

# 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=${SPRING_PROFILES_ACTIVE}", "app.jar"]