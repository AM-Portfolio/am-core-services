# Runtime Image
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Copy JAR pre-built by the CI runner
COPY target/*.jar app.jar

# SpecKit pilot contract packs (am-market-data / am-news / am-parser tools.json)
COPY contract-packs/services /packs/services

# Install curl for healthcheck
RUN apt-get update && \
    apt-get install -y curl && \
    rm -rf /var/lib/apt/lists/* && \
    ln -sf /usr/share/zoneinfo/Asia/Kolkata /etc/localtime

# Environment variables
ENV SPRING_PROFILES_ACTIVE=docker
ENV TZ=Asia/Kolkata
# Contract MCP: full am-specs pack fleet (sync via scripts/sync_contract_packs.py).
ENV AM_CONTRACT_MCP_ENABLED=true
ENV AM_CONTRACT_MCP_PACKS_DIR=/packs/services
ENV AM_CONTRACT_MCP_ENV=dev
ENV AM_CONTRACT_MCP_ALL_GROUPS=true

EXPOSE 8080

# Set permissions for /app
RUN chown -R 1000:1000 /app

# Switch to non-root user
USER 1000

ENTRYPOINT ["java", "-jar", "app.jar"]
