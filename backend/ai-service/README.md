# ai-service

Tool-based AI orchestrator and RAG consumer (OpenAI in later phases).

## Run

Infrastructure must be up (`docker compose up -d` from repo root).

```bash
cd backend
./mvnw -pl ai-service spring-boot:run
```

Health: http://localhost:8084/api/health  
Actuator: http://localhost:8084/actuator/health

## Package layout

`com.threatadvisor.ai` — controller, config, dto, exception, plus empty service/domain/repository/kafka packages for later phases.

Do not put business logic in controllers or Kafka consumers.
