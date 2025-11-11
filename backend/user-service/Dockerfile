# Multi-stage build for User Service
FROM maven:3.9.5-eclipse-temurin-17 AS build

WORKDIR /app

# Copy parent pom first
COPY ../pom.xml ../pom.xml
COPY pom.xml .

# Copy source code
COPY src src

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
EXPOSE 8081

# Set environment variables
ENV JAVA_OPTS=""
ENV CONFIG_SERVER=config-server
ENV EUREKA_SERVER=eureka-server
ENV DB_HOST=postgres
ENV DB_USER=postgres
ENV DB_PASSWORD=postgres

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -sf http://localhost:8081/actuator/health || exit 1

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
