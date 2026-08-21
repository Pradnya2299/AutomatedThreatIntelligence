# notification-service

Notification delivery and in-app records (channels in later phases).

## Run

Infrastructure must be up (`docker compose up -d` from repo root).

```bash
cd backend
./mvnw -pl notification-service spring-boot:run
```

Health: http://localhost:8085/api/health  
Actuator: http://localhost:8085/actuator/health

## Package layout

`com.threatadvisor.notification` — controller, config, dto, exception, plus empty service/domain/repository/kafka packages for later phases.

Do not put business logic in controllers or Kafka consumers.
