package com.threatadvisor.ingestion.pipeline;

import com.threatadvisor.ingestion.repository.CveRawRecordRepository;
import com.threatadvisor.ingestion.repository.EventProcessingRecordRepository;
import com.threatadvisor.ingestion.repository.VulnerabilityCpeRepository;
import com.threatadvisor.ingestion.repository.VulnerabilityRepository;
import com.threatadvisor.ingestion.service.CveNormalizationService;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@DisabledIfEnvironmentVariable(named = "SKIP_TESTCONTAINERS", matches = "true")
class CveIngestionPipelineTest {

    private static final DockerImageName PGVECTOR =
            DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres");

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(PGVECTOR)
            .withDatabaseName("threat_advisor")
            .withUsername("threat_advisor")
            .withPassword("threat_advisor_dev_change_me");

    @Container
    static final KafkaContainer KAFKA = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        Path migrations = findRepoRoot().resolve("database/migrations");
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("filesystem:" + migrations.toAbsolutePath())
                .load()
                .migrate();
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "false");
        registry.add("spring.kafka.listener.missing-topics-fatal", () -> "false");
        registry.add("spring.kafka.admin.fail-fast", () -> "false");
        registry.add("spring.kafka.producer.properties.max.block.ms", () -> "15000");
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private VulnerabilityRepository vulnerabilities;

    @Autowired
    private VulnerabilityCpeRepository cpes;

    @Autowired
    private CveRawRecordRepository rawRecords;

    @Autowired
    private EventProcessingRecordRepository processingRecords;

    @Autowired
    private CveNormalizationService normalizationService;

    @Test
    @SuppressWarnings("rawtypes")
    void ingestNormalizesAndIsIdempotent() throws Exception {
        String body = Files.readString(Path.of(
                findRepoRoot().toString(),
                "backend/ingestion-service/src/main/resources/cve-fixtures/cve-critical.json"));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Correlation-Id", UUID.randomUUID().toString());

        ResponseEntity<Map> first = restTemplate.postForEntity(
                "/internal/ingestion/cve?source=fixture",
                new HttpEntity<>(body, headers),
                Map.class);
        assertEquals(202, first.getStatusCode().value());
        assertEquals("CVE-2024-90001", first.getBody().get("cveId"));
        assertEquals("ACCEPTED", first.getBody().get("status"));

        await().atMost(java.time.Duration.ofSeconds(30)).untilAsserted(() -> {
            assertEquals(1, vulnerabilities.countByCveId("CVE-2024-90001"));
            var vuln = vulnerabilities.findByCveId("CVE-2024-90001").orElseThrow();
            assertEquals("CRITICAL", vuln.getSeverity());
            assertTrue(cpes.countByVulnerabilityId(vuln.getId()) >= 1);
            assertEquals("NORMALIZED", rawRecords.findBySourceAndExternalId("fixture", "CVE-2024-90001").orElseThrow().getProcessingStatus());
            assertTrue(processingRecords.existsByEventIdAndConsumer(
                    UUID.fromString(first.getBody().get("eventId").toString()),
                    CveNormalizationService.CONSUMER));
        });

        UUID eventId = UUID.fromString(first.getBody().get("eventId").toString());
        String replay = """
                {"eventId":"%s","eventType":"cve.raw","eventVersion":1,"timestamp":"2024-01-01T00:00:00Z","source":"ingestion-service","correlationId":"%s","payload":{"cveId":"CVE-2024-90001"}}
                """.formatted(eventId, first.getBody().get("correlationId"));
        normalizationService.handleRawEvent(replay);
        assertEquals(1, vulnerabilities.countByCveId("CVE-2024-90001"));

        ResponseEntity<Map> second = restTemplate.postForEntity(
                "/internal/ingestion/cve?source=fixture",
                new HttpEntity<>(body, headers),
                Map.class);
        assertEquals(200, second.getStatusCode().value());
        assertEquals("DUPLICATE", second.getBody().get("status"));
        assertEquals(first.getBody().get("eventId"), second.getBody().get("eventId"));
        assertEquals(1, vulnerabilities.countByCveId("CVE-2024-90001"));
        assertEquals(1, rawRecords.findBySourceAndExternalId("fixture", "CVE-2024-90001").stream().count());
    }

    private static Path findRepoRoot() {
        Path current = Path.of("").toAbsolutePath();
        for (int i = 0; i < 8; i++) {
            if (Files.isDirectory(current.resolve("database/migrations"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("database/migrations not found");
    }
}
