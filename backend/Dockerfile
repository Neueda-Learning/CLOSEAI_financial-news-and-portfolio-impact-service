# Multi-stage: the build layer carries Maven and the full JDK, the runtime
# layer carries neither. Keeps the shipped image a fraction of the size.

# ---- build ----------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# Copy the POM alone first so dependency resolution is cached independently of
# source changes - editing a Java file then does not re-download the world.
COPY pom.xml .

COPY checkstyle.xml .
COPY src ./src
RUN mvn -B clean package -DskipTests

# ---- runtime --------------------------------------------------------------
FROM eclipse-temurin:17-jre AS runtime
WORKDIR /app

# Non-root: a container process has no reason to run as root
RUN useradd --system --uid 1001 --create-home appuser
USER appuser

COPY --from=build --chown=appuser:appuser /build/target/*.jar app.jar

EXPOSE 8080

# Container memory, not the host's, decides the heap
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
