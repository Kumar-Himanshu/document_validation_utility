# ---- Build stage -----------------------------------------------------------
# Uses Java 21 (as the BRD requests) to build the fat jar. The pom targets
# bytecode level 17, so the artifact runs on both 17 and 21 runtimes.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# Cache dependencies first for faster incremental builds.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -q -B dependency:go-offline

# Build and package, skipping tests (tests run in CI, not in the image build).
COPY src ./src
RUN ./mvnw -q -B clean package -DskipTests

# ---- Runtime stage ---------------------------------------------------------
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

# Create and run as a dedicated non-root user.
RUN groupadd --system appgroup && useradd --system --gid appgroup --no-create-home appuser

# Copy only the built jar from the build stage.
COPY --from=build /workspace/target/document-validation-utility-*.jar app.jar

# Drop privileges.
USER appuser:appgroup

EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS=""

# Container health check hits the actuator liveness probe.
HEALTHCHECK --interval=30s --timeout=3s --start-period=20s --retries=3 \
  CMD ["sh", "-c", "wget -qO- http://localhost:8080/actuator/health/liveness || exit 1"]

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
