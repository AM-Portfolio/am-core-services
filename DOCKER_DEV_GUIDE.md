# Docker Development Guide - AM Backend Platform

This guide explains how to run the **AM Gateway** and **AM Analysis** services in Docker development mode.

## Overview

The `docker-compose.dev.yml` configuration runs two services:
1. **am-gateway** - WebSocket Gateway Service (Port 8091)
2. **am-analysis** - Analysis Service (Port 8093)

Both services:
- Use the same base image (`eclipse-temurin:17-jre-focal`)
- Mount locally built JAR files from their respective `target/` directories
- Connect to the shared `am-net` Docker network for communication with Kafka and MongoDB
- Auto-restart on failure
- Include health checks

## Prerequisites

1. **Infrastructure Running**: Ensure the `am-infra` stack is running with Kafka and MongoDB:
   ```bash
   cd ../am-infra
   docker-compose up -d
   ```

2. **Services Built**: Build both services locally:
   ```bash
   # From am-backend-platform directory
   mvn clean package -DskipTests
   ```
   
   This creates:
   - `./services/am-gateway/target/am-gateway-1.0.0-SNAPSHOT.jar`
   - `./services/am-analysis/target/am-analysis-1.0.0-SNAPSHOT.jar`

## Running the Services

### Start Both Services
```bash
docker-compose -f docker-compose.dev.yml up -d
```

### Start a Single Service
```bash
# Only Gateway
docker-compose -f docker-compose.dev.yml up -d am-gateway

# Only Analysis
docker-compose -f docker-compose.dev.yml up -d am-analysis
```

### View Logs
```bash
# All services
docker-compose -f docker-compose.dev.yml logs -f

# Specific service
docker-compose -f docker-compose.dev.yml logs -f am-gateway
docker-compose -f docker-compose.dev.yml logs -f am-analysis
```

### Stop Services
```bash
docker-compose -f docker-compose.dev.yml down
```

## Service Details

### AM Gateway (WebSocket Gateway)
- **Container Name**: `am-gateway-dev`
- **Port**: 8091
- **JAR Path**: `./services/am-gateway/target/am-gateway-1.0.0-SNAPSHOT.jar`
- **Purpose**: WebSocket connections for real-time updates
- **Health Check**: `http://localhost:8091/actuator/health`

### AM Analysis
- **Container Name**: `am-analysis-dev`
- **Port**: 8093
- **JAR Path**: `./services/am-analysis/target/am-analysis-1.0.0-SNAPSHOT.jar`
- **Purpose**: Trade analysis and processing
- **MongoDB Database**: `analysis`
- **Health Check**: `http://localhost:8093/actuator/health`

## Environment Configuration

The `.env.dev` file contains shared configuration:

```bash
SPRING_PROFILES_ACTIVE=dev
SPRING_KAFKA_BOOTSTRAP_SERVERS=am_kafka:9092
SPRING_DATA_MONGODB_URI=mongodb://am_mongodb:27017/am_gateway
SPRING_DATA_MONGODB_URI_ANALYSIS=mongodb://admin:password123@am_mongodb:27017/analysis?authSource=admin
```

### Key Points:
- Both services use the **dev** Spring profile
- Kafka connection: `am_kafka:9092` (internal Docker network)
- Gateway uses database: `am_gateway`
- Analysis uses database: `analysis` with authentication

## Development Workflow

### Hot Reload Pattern
When you update code:

1. **Rebuild the specific service**:
   ```bash
   # For gateway
   mvn package -pl services/am-gateway -DskipTests
   
   # For analysis
   mvn package -pl services/am-analysis -DskipTests
   ```

2. **Restart the container**:
   ```bash
   docker-compose -f docker-compose.dev.yml restart am-gateway
   # OR
   docker-compose -f docker-compose.dev.yml restart am-analysis
   ```

The volume mount ensures the new JAR is immediately available in the container.

## Network Architecture

```
┌─────────────────────────────────────┐
│      am-infra_am-net Network        │
│                                     │
│  ┌──────────┐    ┌──────────┐      │
│  │  Kafka   │    │ MongoDB  │      │
│  │  :9092   │    │  :27017  │      │
│  └────┬─────┘    └────┬─────┘      │
│       │               │             │
│  ┌────┴───────────────┴─────┐      │
│  │                           │      │
│  │  ┌─────────────────┐     │      │
│  │  │  AM Gateway     │     │      │
│  │  │  :8091          │     │      │
│  │  └─────────────────┘     │      │
│  │                           │      │
│  │  ┌─────────────────┐     │      │
│  │  │  AM Analysis    │     │      │
│  │  │  :8093          │     │      │
│  │  └─────────────────┘     │      │
│  │                           │      │
│  └───────────────────────────┘      │
└─────────────────────────────────────┘
```

## Port Reference

| Service | Container Port | Host Port | Access URL |
|---------|---------------|-----------|------------|
| Gateway | 8091 | 8091 | http://localhost:8091 |
| Analysis | 8093 | 8093 | http://localhost:8093 |

## Troubleshooting

### Services Won't Start
1. **Check infrastructure is running**:
   ```bash
   docker ps | grep -E 'kafka|mongodb'
   ```

2. **Check network exists**:
   ```bash
   docker network ls | grep am-infra_am-net
   ```

3. **View service logs**:
   ```bash
   docker-compose -f docker-compose.dev.yml logs
   ```

### JAR Not Found
Ensure you've built the services:
```bash
mvn clean package -DskipTests
ls -la services/am-gateway/target/*.jar
ls -la services/am-analysis/target/*.jar
```

### Database Connection Issues
Verify MongoDB is accessible:
```bash
docker exec -it am_mongodb mongosh -u admin -p password123
```

### Health Check Failures
Check service health:
```bash
curl http://localhost:8091/actuator/health
curl http://localhost:8093/actuator/health
```

## Next Steps

- **Production Build**: Use `docker-compose.yml` for full multi-stage builds
- **Traefik Integration**: Configure reverse proxy for external access
- **Monitoring**: Add Prometheus metrics endpoints
- **Scaling**: Use `docker-compose up --scale` for multiple instances
