# Stage 1: Build
FROM maven:3.8.5-openjdk-17-slim AS build

# Build arguments for GitHub authentication
ARG GITHUB_PACKAGES_USERNAME
ARG GITHUB_PACKAGES_TOKEN

WORKDIR /app

# Configure Maven settings for GitHub Packages
RUN mkdir -p /root/.m2 && \
    echo '<?xml version="1.0" encoding="UTF-8"?>' > /root/.m2/settings.xml && \
    echo '<settings>' >> /root/.m2/settings.xml && \
    echo '  <servers>' >> /root/.m2/settings.xml && \
    echo '    <server>' >> /root/.m2/settings.xml && \
    echo '      <id>github-investment</id>' >> /root/.m2/settings.xml && \
    echo "      <username>${GITHUB_PACKAGES_USERNAME}</username>" >> /root/.m2/settings.xml && \
    echo "      <password>${GITHUB_PACKAGES_TOKEN}</password>" >> /root/.m2/settings.xml && \
    echo '    </server>' >> /root/.m2/settings.xml && \
    echo '    <server>' >> /root/.m2/settings.xml && \
    echo '      <id>github</id>' >> /root/.m2/settings.xml && \
    echo "      <username>${GITHUB_PACKAGES_USERNAME}</username>" >> /root/.m2/settings.xml && \
    echo "      <password>${GITHUB_PACKAGES_TOKEN}</password>" >> /root/.m2/settings.xml && \
    echo '    </server>' >> /root/.m2/settings.xml && \
    echo '  </servers>' >> /root/.m2/settings.xml && \
    echo '</settings>' >> /root/.m2/settings.xml

# Copy the entire monorepo to resolve internal dependencies
COPY . .

# Build only the am-gateway module and its dependencies
RUN mvn clean package -pl services/am-gateway -am -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-focal
WORKDIR /app

# Copy the built JAR for am-gateway
COPY --from=build /app/services/am-gateway/target/*.jar app.jar

# Set timezone
RUN ln -sf /usr/share/zoneinfo/Asia/Kolkata /etc/localtime
ENV TZ=Asia/Kolkata

EXPOSE 8091

ENTRYPOINT ["java", "-jar", "app.jar"]
