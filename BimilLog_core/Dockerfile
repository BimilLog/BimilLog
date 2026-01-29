# 로컬 테스트용 도커 파일
# Stage 1: Build
FROM amazoncorretto:21-alpine-jdk AS builder

WORKDIR /app

# Copy Gradle wrapper and configuration files
COPY gradlew ./
COPY gradle ./gradle
COPY build.gradle settings.gradle ./

# Fix line endings (Windows CRLF → Unix LF) and download dependencies
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew && ./gradlew dependencies --no-daemon || true

# Copy source code
COPY src ./src

# Build application (clean first to avoid QueryDSL duplicate generation)
RUN ./gradlew clean bootJar --no-daemon -x test

# Stage 2: Runtime
FROM amazoncorretto:21-alpine-jdk

WORKDIR /app

# Create non-root user
RUN addgroup -S spring && adduser -S spring -G spring

# Create logs directory with proper permissions
RUN mkdir -p /app/logs && chown spring:spring /app/logs

# Download Scouter Java Agent
ARG SCOUTER_VERSION=2.21.1
RUN mkdir -p /app/scouter && \
    wget -q "https://github.com/scouter-project/scouter/releases/download/v${SCOUTER_VERSION}/scouter-all-${SCOUTER_VERSION}.tar.gz" -O /tmp/scouter.tar.gz && \
    tar -xzf /tmp/scouter.tar.gz -C /tmp && \
    cp /tmp/scouter/agent.java/scouter.agent.jar /app/scouter/ && \
    rm -rf /tmp/scouter /tmp/scouter.tar.gz && \
    chown -R spring:spring /app/scouter

USER spring:spring

# Copy JAR from builder
COPY --from=builder /app/build/libs/*.jar app.jar

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Run application with local profile
# Scouter agent is attached via JAVA_TOOL_OPTIONS env var in docker-compose
ENTRYPOINT ["java", "-Dspring.profiles.active=local", "-jar", "app.jar"]
