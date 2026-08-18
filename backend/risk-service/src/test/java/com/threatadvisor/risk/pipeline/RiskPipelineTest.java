package com.threatadvisor.risk.pipeline;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.threatadvisor.risk.domain.Asset;
import com.threatadvisor.risk.domain.Finding;
import com.threatadvisor.risk.domain.Organization;
import com.threatadvisor.risk.domain.RiskAssessment;
import com.threatadvisor.risk.domain.Vulnerability;
import com.threatadvisor.risk.kafka.EventEnvelope;
import com.threatadvisor.risk.repository.AssetRepository;
import com.threatadvisor.risk.repository.EventProcessingRecordRepository;
import com.threatadvisor.risk.repository.FindingRepository;
import com.threatadvisor.risk.repository.OrganizationRepository;
import com.threatadvisor.risk.repository.RiskAssessmentRepository;
import com.threatadvisor.risk.repository.VulnerabilityRepository;
import com.threatadvisor.risk.service.RiskService;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers(disabledWithoutDocker = true)
@DisabledIfEnvironmentVariable(named = "SKIP_TESTCONTAINERS", matches = "true")
class RiskPipelineTest {

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
    private RiskService riskService;
    @Autowired
    private OrganizationRepository organizations;
    @Autowired
    private AssetRepository assets;
    @Autowired
    private VulnerabilityRepository vulnerabilities;
    @Autowired
    private FindingRepository findings;
    @Autowired
    private RiskAssessmentRepository assessments;
    @Autowired
    private EventProcessingRecordRepository processingRecords;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    private UUID orgId;
    private UUID criticalFindingId;
    private UUID mediumFindingId;

    @BeforeEach
    void seed() {
        assessments.deleteAll();
        processingRecords.deleteAll();
        findings.deleteAll();
        vulnerabilities.deleteAll();
        assets.deleteAll();
        organizations.deleteAll();

        Instant now = Instant.now();
        orgId = UUID.randomUUID();
        Organization org = new Organization();
        org.setId(orgId);
        org.setName("Northwind");
        org.setSlug("northwind-risk");
        org.setCreatedAt(now);
        org.setUpdatedAt(now);
        organizations.save(org);

        UUID edge = asset("nw-prod-edge-gw-01", "CRITICAL", true);
        UUID laptop = asset("nw-dev-win-01", "MEDIUM", false);
        UUID log4j = vulnerability("CVE-2021-44228", new BigDecimal("10.0"), true, true);
        UUID smartscreen = vulnerability("CVE-2023-36025", new BigDecimal("5.4"), false, false);
        criticalFindingId = finding(edge, log4j);
        mediumFindingId = finding(laptop, smartscreen);
    }

    @Test
    void criticalFindingScoresCriticalAndIsIdempotent() throws Exception {
        RiskService.RiskOutcome first = riskService.handleFindingCreated(envelope(criticalFindingId));
        assertEquals("COMPLETED", first.status());
        assertEquals("CRITICAL", first.result().level());
        assertEquals(new BigDecimal("97.50"), first.result().score());
        assertEquals(1, assessments.countByFindingId(criticalFindingId));
        RiskAssessment row = assessments.findByFindingId(criticalFindingId).orElseThrow();
        assertTrue(row.getReasons().contains("internet-facing"));

        RiskService.RiskOutcome duplicate = riskService.handleFindingCreated(
                envelope(criticalFindingId, first.eventId()));
        assertEquals("DUPLICATE", duplicate.status());
        assertEquals(1, assessments.countByFindingId(criticalFindingId));
        assertTrue(processingRecords.existsByEventIdAndConsumer(first.eventId(), RiskService.CONSUMER));
    }

    @Test
    void mediumInternalFindingIsMedium() throws Exception {
        RiskService.RiskOutcome outcome = riskService.handleFindingCreated(envelope(mediumFindingId));
        assertEquals("MEDIUM", outcome.result().level());
        assertEquals(new BigDecimal("34.10"), outcome.result().score());
        assertEquals(1, assessments.countByFindingId(mediumFindingId));
    }

    @Test
    void kafkaConsumerWritesAssessment() throws Exception {
        UUID eventId = UUID.randomUUID();
        kafkaTemplate.send(EventEnvelope.TYPE_FINDING_CREATED, criticalFindingId.toString(),
                envelope(criticalFindingId, eventId)).get();
        await().atMost(java.time.Duration.ofSeconds(30)).untilAsserted(() -> {
            assertEquals(1, assessments.countByFindingId(criticalFindingId));
            assertTrue(processingRecords.existsByEventIdAndConsumer(eventId, RiskService.CONSUMER));
        });
    }

    private String envelope(UUID findingId) throws Exception {
        return envelope(findingId, UUID.randomUUID());
    }

    private String envelope(UUID findingId, UUID eventId) throws Exception {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("findingId", findingId.toString());
        EventEnvelope envelope = new EventEnvelope(
                eventId,
                EventEnvelope.TYPE_FINDING_CREATED,
                1,
                Instant.now(),
                "correlation-service",
                UUID.randomUUID(),
                payload);
        return objectMapper.writeValueAsString(envelope);
    }

    private UUID asset(String hostname, String criticality, boolean internet) {
        Instant now = Instant.now();
        Asset asset = new Asset();
        asset.setId(UUID.randomUUID());
        asset.setOrganizationId(orgId);
        asset.setHostname(hostname);
        asset.setEnvironment("PRODUCTION");
        asset.setBusinessCriticality(criticality);
        asset.setInternetExposure(internet);
        asset.setStatus("ACTIVE");
        asset.setCreatedAt(now);
        asset.setUpdatedAt(now);
        return assets.save(asset).getId();
    }

    private UUID vulnerability(String cveId, BigDecimal cvss, boolean exploit, boolean active) {
        Instant now = Instant.now();
        Vulnerability vuln = new Vulnerability();
        vuln.setId(UUID.randomUUID());
        vuln.setCveId(cveId);
        vuln.setCvssScore(cvss);
        vuln.setExploitAvailable(exploit);
        vuln.setActivelyExploited(active);
        vuln.setMetadata("{}");
        vuln.setCvssMetrics("{}");
        vuln.setCreatedAt(now);
        vuln.setUpdatedAt(now);
        return vulnerabilities.save(vuln).getId();
    }

    private UUID finding(UUID assetId, UUID vulnerabilityId) {
        Instant now = Instant.now();
        Finding finding = new Finding();
        finding.setId(UUID.randomUUID());
        finding.setOrganizationId(orgId);
        finding.setAssetId(assetId);
        finding.setVulnerabilityId(vulnerabilityId);
        finding.setStatus("OPEN");
        finding.setMatchExplanation("{\"text\":\"test\"}");
        finding.setDetectedAt(now);
        finding.setCreatedAt(now);
        finding.setUpdatedAt(now);
        return findings.save(finding).getId();
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
