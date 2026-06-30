# syntax=docker/dockerfile:1.7

FROM eclipse-temurin:21-jdk AS builder
WORKDIR /workspace

COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN chmod +x ./gradlew && ./gradlew --no-daemon dependencies > /dev/null 2>&1 || true

COPY src ./src
RUN ./gradlew --no-daemon clean bootJar -x test

FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

RUN groupadd -r app && useradd -r -g app -u 10001 app \
 && apt-get update && apt-get install -y --no-install-recommends curl tini \
 && rm -rf /var/lib/apt/lists/*

# 부트 fat jar 를 그대로 실행 (레이어 추출/JarLauncher 방식은 Boot 4 추출 레이아웃과 불일치)
COPY --from=builder /workspace/build/libs/*.jar app.jar

USER app
EXPOSE 8080

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+UseG1GC -XX:+ExitOnOutOfMemoryError"
ENV SPRING_PROFILES_ACTIVE=prod

ENTRYPOINT ["/usr/bin/tini", "--", "java", "-jar", "/app/app.jar"]
