# AM Backend Platform - Run Guide

Here are the three ways to run the backend platform (`am-gateway`).

## 1. Development Mode (Fast Iteration)
Use this when you are editing code. It mounts your local `target/*.jar` file directly into the container.

**Prerequisites:**
- Run `mvn package -DskipTests` locally first.

**Command:**
```powershell
# Navigate to project root
cd a:\InfraCode\AM-Portfolio-grp\am-backend-platform

# Run with Dev Compose
docker-compose -f docker-compose.dev.yml up -d
```
*Note: This skips the Docker build and uses your local jar file immediately.*

---

## 2. Deployment Mode (Production/Clean)
Use this to build the official Docker image from scratch.

**Command:**
```powershell
# Navigate to project root
cd a:\InfraCode\AM-Portfolio-grp\am-backend-platform

# Build and Run
docker-compose up -d --build
```
*Note: This runs the `Dockerfile`, which compiles the code inside the image.*

---

## 3. Manual Mode (No Docker)
Use this to run the jar directly on your Windows machine (Host).

**Prerequisites:**
- Ensure Infrastructure (Mongo, Postgres, Kafka) is running and exposing ports to localhost (27017, 5432, 9092).

**Command:**
```powershell
# Navigate to project root
cd a:\InfraCode\AM-Portfolio-grp\am-backend-platform

# 1. Build
mvn clean package -DskipTests

# 2. Run
java -jar services/am-gateway/target/am-gateway-1.0.0-SNAPSHOT.jar
```
*Note: Since the default `application.yml` points to `localhost:9092`, this works natively on Windows.*
