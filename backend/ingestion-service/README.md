# ingestion-service

CVE intake, normalization, enrichment (engines in later phases).

## Run

Infrastructure must be up (`docker compose up -d` from repo root).

```bash
cd backend
./mvnw -pl ingestion-service spring-boot:run
```

Health: http://localhost:8081/api/health  
Actuator: http://localhost:8081/actuator/health

## Package layout

`com.threatadvisor.ingestion` — controller, config, dto, exception, plus empty service/domain/repository/kafka packages for later phases.

Do not put business logic in controllers or Kafka consumers.
