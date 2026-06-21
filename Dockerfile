FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

RUN groupadd -r app && useradd -r -g app -u 10001 app \
 && apt-get update && apt-get install -y --no-install-recommends curl tini \
 && rm -rf /var/lib/apt/lists/*

COPY build/libs/*.jar app.jar

USER app
EXPOSE 8080

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=50 -XX:+UseG1GC -XX:+ExitOnOutOfMemoryError"
ENV SPRING_PROFILES_ACTIVE=prod

ENTRYPOINT ["/usr/bin/tini", "--", "java", "-jar", "/app/app.jar"]
