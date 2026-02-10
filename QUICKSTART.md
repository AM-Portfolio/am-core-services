# Quick Start Commands

## Build Both Services
```bash
cd a:\InfraCode\AM-Portfolio-grp\am-backend-platform
mvn clean package -DskipTests
```

## Run in Docker Dev Mode
```bash
# Start both services
docker-compose -f docker-compose.dev.yml up -d

# View logs
docker-compose -f docker-compose.dev.yml logs -f
```

## Access Services
- Gateway: http://localhost:8091
- Analysis: http://localhost:8090

## Stop Services
```bash
docker-compose -f docker-compose.dev.yml down
```

See [DOCKER_DEV_GUIDE.md](DOCKER_DEV_GUIDE.md) for detailed documentation.
