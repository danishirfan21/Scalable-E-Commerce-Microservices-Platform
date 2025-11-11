# Multi-stage build for Eureka Server
FROM maven:3.9.5-eclipse-temurin-17 AS build

WORKDIR /app

# Copy and install parent POM first (before service POM to avoid resolution issues)
COPY pom.xml ./parent-pom.xml
RUN mvn -N install:install-file \
  -Dfile=parent-pom.xml \
  -DpomFile=parent-pom.xml

# Copy service POM and source code
COPY eureka-server/pom.xml ./pom.xml
COPY eureka-server/src ./src

# Build the application
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre-alpine

# Install curl for healthchecks
RUN apk add --no-cache curl

WORKDIR /app

# Copy the built artifact from the build stage
COPY --from=build /app/target/*.jar app.jar

# Expose port
EXPOSE 8761

# Set environment variables
ENV JAVA_OPTS=""

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -sf http://localhost:8761/actuator/health || exit 1

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
