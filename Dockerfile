FROM maven:3.8.5-openjdk-17-slim AS build
WORKDIR /app

# Copy the entire monorepo to ensure parent/child relationships and dependencies are resolved
COPY . .

# Build the entire project (skipping tests to save time)
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre-focal
WORKDIR /app

# Copy the built artifact for am-gateway
COPY --from=build /app/services/am-gateway/target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
