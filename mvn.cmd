@echo off
setlocal enabledelayedexpansion
set args=%*
set args=!args:C:\Asrax\Asrax\am-core-services=/app!
set args=!args:\=/!
docker run --rm -v "C:\Asrax\Asrax\am-core-services:/app" -v "%USERPROFILE%\.m2:/root/.m2" -e GHCR_TOKEN="%GHCR_TOKEN%" -w /app maven:3.9-eclipse-temurin-17 mvn !args!
