
# ---- Build stage -----------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# Cache Maven dependencies first.
COPY pom.xml ./
RUN mvn -q -B dependency:go-offline

# Build and package the application.
COPY src ./src
RUN mvn -q -B clean package -DskipTests

# ---- Runtime stage ---------------------------------------------------------
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

# Run as a dedicated non-root user.
RUN groupadd --system appgroup && \
    useradd --system --gid appgroup --no-create-home appuser

# Copy the executable application JAR.
COPY --from=build /workspace/target/document-validation-utility-*.jar app.jar

USER appuser:appgroup

EXPOSE 8080

ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS=""

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
