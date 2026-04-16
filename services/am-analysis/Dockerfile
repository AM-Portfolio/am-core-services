# Stage 1: Build
FROM maven:3.8.5-openjdk-17-slim AS build
WORKDIR /app

# Copy the entire monorepo to resolve internal dependencies
COPY . .

# Build only the am-analysis module and its dependencies
RUN mvn clean package -pl services/am-analysis -am -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-focal
WORKDIR /app

# Copy the built JAR for am-analysis
COPY --from=build /app/services/am-analysis/target/*.jar app.jar

# Set timezone
RUN ln -sf /usr/share/zoneinfo/Asia/Kolkata /etc/localtime
ENV TZ=Asia/Kolkata

EXPOSE 8090

ENTRYPOINT ["java", "-jar", "app.jar"]
