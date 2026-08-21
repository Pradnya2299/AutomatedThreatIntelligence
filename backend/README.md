# Backend

Maven multi-module parent. Each module is an independently runnable Spring Boot 3.4 application (Java 21).

```bash
./mvnw test
./mvnw -pl api-service spring-boot:run
```

See the root README and each service README. Kafka consumers and domain engines are not implemented in Phase 1.
