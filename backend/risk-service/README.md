# risk-service

Deterministic risk engine (formula in later phases).

## Run

Infrastructure must be up (`docker compose up -d` from repo root).

```bash
cd backend
./mvnw -pl risk-service spring-boot:run
```

Health: http://localhost:8083/api/health  
Actuator: http://localhost:8083/actuator/health

## Package layout

`com.threatadvisor.risk` — controller, config, dto, exception, plus empty service/domain/repository/kafka packages for later phases.

Do not put business logic in controllers or Kafka consumers.
