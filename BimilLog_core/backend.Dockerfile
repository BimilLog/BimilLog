# 1. 빌드 단계 (Build Stage)
FROM eclipse-temurin:21-jdk AS builder
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

# 애플리케이션 빌드 (테스트 완전 제외)
RUN ./gradlew assemble --no-daemon

# ---

# 2. 실행 단계 (Production Stage)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# 빌드 단계(builder)에서 생성된 JAR 파일만 복사 (와일드카드로 버전 독립적)
COPY --from=builder /app/build/libs/*.jar app.jar

# 운영 환경 프로파일 활성화 (필요시 외부에서 변경 가능)
ENV SPRING_PROFILES_ACTIVE=prod

# 로그 디렉토리 생성
RUN mkdir -p /app/logs

# 애플리케이션 실행 (운영 환경 최적화 옵션 포함)
ENTRYPOINT ["java", \
    "-Dobj_name=BimilLog", \
    "-Dfile.encoding=UTF-8", \
    "-Duser.timezone=Asia/Seoul", \
    "-XX:+HeapDumpOnOutOfMemoryError", \
    "-Dspring.profiles.active=${SPRING_PROFILES_ACTIVE}", \
    "-jar", "app.jar"]