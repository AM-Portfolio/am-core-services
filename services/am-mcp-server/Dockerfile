# Stage 1: Build
FROM maven:3.8.5-openjdk-17-slim AS build
WORKDIR /app

# Copy the entire monorepo to resolve internal dependencies
COPY . .

# Build only the am-mcp-server module and its dependencies
RUN mvn clean package -pl services/am-mcp-server -am -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-focal
WORKDIR /app

# Copy the built JAR for am-mcp-server
COPY --from=build /app/services/am-mcp-server/target/*.jar app.jar

# Set timezone
RUN ln -sf /usr/share/zoneinfo/Asia/Kolkata /etc/localtime
ENV TZ=Asia/Kolkata

# Default Spring Boot port if not explicitly set in YAML
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
