# correlation-service

Deterministic CVE-to-asset correlation (engine in later phases).

## Run

Infrastructure must be up (`docker compose up -d` from repo root).

```bash
cd backend
./mvnw -pl correlation-service spring-boot:run
```

Health: http://localhost:8082/api/health  
Actuator: http://localhost:8082/actuator/health

## Package layout

`com.threatadvisor.correlation` — controller, config, dto, exception, plus empty service/domain/repository/kafka packages for later phases.

Do not put business logic in controllers or Kafka consumers.
