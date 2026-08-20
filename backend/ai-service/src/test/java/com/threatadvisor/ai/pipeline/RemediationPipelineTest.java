package com.threatadvisor.ai.pipeline;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.threatadvisor.ai.domain.Asset;
import com.threatadvisor.ai.domain.AssetSoftware;
import com.threatadvisor.ai.domain.Finding;
import com.threatadvisor.ai.domain.Organization;
import com.threatadvisor.ai.domain.RemediationPlan;
import com.threatadvisor.ai.domain.RiskAssessment;
import com.threatadvisor.ai.domain.Vulnerability;
import com.threatadvisor.ai.kafka.EventEnvelope;
import com.threatadvisor.ai.rag.KnowledgeIngestionService;
import com.threatadvisor.ai.repository.AssetRepository;
import com.threatadvisor.ai.repository.AssetSoftwareRepository;
import com.threatadvisor.ai.repository.EventProcessingRecordRepository;
import com.threatadvisor.ai.repository.FindingRepository;
import com.threatadvisor.ai.repository.OrganizationRepository;
import com.threatadvisor.ai.repository.RemediationPlanRepository;
import com.threatadvisor.ai.repository.RiskAssessmentRepository;
import com.threatadvisor.ai.repository.VulnerabilityRepository;
import com.threatadvisor.ai.service.RemediationGenerationService;
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
class RemediationPipelineTest {

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
        registry.add("ai.demo-mode", () -> "true");
        registry.add("ai.ingest-on-startup", () -> "false");
        registry.add("spring.kafka.listener.missing-topics-fatal", () -> "false");
        registry.add("spring.kafka.admin.fail-fast", () -> "false");
        registry.add("spring.autoconfigure.exclude",
                () -> "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration");
    }

    @Autowired
    private RemediationGenerationService generationService;
    @Autowired
    private KnowledgeIngestionService ingestionService;
    @Autowired
    private OrganizationRepository organizations;
    @Autowired
    private AssetRepository assets;
    @Autowired
    private AssetSoftwareRepository software;
    @Autowired
    private VulnerabilityRepository vulnerabilities;
    @Autowired
    private FindingRepository findings;
    @Autowired
    private RiskAssessmentRepository risks;
    @Autowired
    private RemediationPlanRepository plans;
    @Autowired
    private EventProcessingRecordRepository processingRecords;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    private UUID findingId;
    private UUID riskId;

    @BeforeEach
    void seed() {
        plans.deleteAll();
        processingRecords.deleteAll();
        risks.deleteAll();
        findings.deleteAll();
        software.deleteAll();
        vulnerabilities.deleteAll();
        assets.deleteAll();
        organizations.deleteAll();
        ingestionService.ingestClasspath();

        Instant now = Instant.now();
        Organization org = new Organization();
        org.setId(UUID.randomUUID());
        org.setName("Northwind");
        org.setSlug("northwind-ai");
        org.setCreatedAt(now);
        org.setUpdatedAt(now);
        organizations.save(org);

        Asset asset = new Asset();
        asset.setId(UUID.randomUUID());
        asset.setOrganizationId(org.getId());
        asset.setHostname("web-prod-01");
        asset.setOperatingSystem("Linux");
        asset.setEnvironment("PRODUCTION");
        asset.setBusinessCriticality("CRITICAL");
        asset.setInternetExposure(true);
        asset.setStatus("ACTIVE");
        asset.setCreatedAt(now);
        asset.setUpdatedAt(now);
        assets.save(asset);

        Vulnerability vuln = new Vulnerability();
        vuln.setId(UUID.randomUUID());
        vuln.setCveId("CVE-2021-44228");
        vuln.setDescription("Log4Shell");
        vuln.setCvssScore(new BigDecimal("10.0"));
        vuln.setSeverity("CRITICAL");
        vuln.setMetadata("{}");
        vuln.setCvssMetrics("{}");
        vuln.setCreatedAt(now);
        vuln.setUpdatedAt(now);
        vulnerabilities.save(vuln);

        AssetSoftware row = new AssetSoftware();
        row.setId(UUID.randomUUID());
        row.setAssetId(asset.getId());
        row.setVendor("apache");
        row.setProduct("log4j");
        row.setVersion("2.14.1");
        row.setInstallationStatus("INSTALLED");
        software.save(row);

        Finding finding = new Finding();
        finding.setId(UUID.randomUUID());
        finding.setOrganizationId(org.getId());
        finding.setAssetId(asset.getId());
        finding.setVulnerabilityId(vuln.getId());
        finding.setStatus("OPEN");
        finding.setMatchExplanation("{\"text\":\"match\"}");
        finding.setDetectedAt(now);
        finding.setCreatedAt(now);
        finding.setUpdatedAt(now);
        findings.save(finding);
        findingId = finding.getId();

        RiskAssessment risk = new RiskAssessment();
        risk.setId(UUID.randomUUID());
        risk.setFindingId(finding.getId());
        risk.setTechnicalRisk(new BigDecimal("100.00"));
        risk.setExploitabilityScore(new BigDecimal("75.00"));
        risk.setExposureScore(new BigDecimal("100.00"));
        risk.setBusinessImpactScore(new BigDecimal("100.00"));
        risk.setAssetCriticalityScore(new BigDecimal("100.00"));
        risk.setFinalRiskScore(new BigDecimal("97.50"));
        risk.setRiskLevel("CRITICAL");
        risk.setFormulaVersion("v1");
        risk.setReasons("{\"text\":\"Risk is CRITICAL (97.50)\"}");
        risk.setCalculatedAt(now);
        risks.save(risk);
        riskId = risk.getId();
    }

    @Test
    void generatesPlanAndSkipsDuplicateEvent() throws Exception {
        UUID eventId = UUID.randomUUID();
        String json = envelopeWithEvent(eventId);
        RemediationGenerationService.Outcome first = generationService.handleRiskCalculated(json);
        assertEquals("COMPLETED", first.status());
        assertEquals(1, plans.countByFindingIdAndRiskAssessmentId(findingId, riskId));
        RemediationPlan plan = plans.findByFindingIdAndRiskAssessmentId(findingId, riskId).orElseThrow();
        assertEquals("GENERATED", plan.getStatus());
        assertEquals("IMMEDIATE", plan.getPriority());
        assertTrue(plan.getSummary().contains("DEMO MODE"));
        assertEquals("demo-deterministic", plan.getModelName());

        RemediationGenerationService.Outcome second = generationService.handleRiskCalculated(json);
        assertEquals("DUPLICATE", second.status());
        assertEquals(1, plans.countByFindingIdAndRiskAssessmentId(findingId, riskId));
    }

    @Test
    void kafkaConsumerWritesPlan() throws Exception {
        UUID eventId = UUID.randomUUID();
        kafkaTemplate.send(EventEnvelope.TYPE_RISK_CALCULATED, findingId.toString(), envelopeWithEvent(eventId)).get();
        await().atMost(java.time.Duration.ofSeconds(30)).untilAsserted(() -> {
            assertEquals(1, plans.countByFindingIdAndRiskAssessmentId(findingId, riskId));
            assertTrue(processingRecords.existsByEventIdAndConsumer(eventId, RemediationGenerationService.CONSUMER));
        });
    }

    private String envelope() throws Exception {
        return envelopeWithEvent(UUID.randomUUID());
    }

    private String envelopeWithEvent(UUID eventId) throws Exception {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("findingId", findingId.toString());
        payload.put("riskAssessmentId", riskId.toString());
        payload.put("cveId", "CVE-2021-44228");
        EventEnvelope envelope = new EventEnvelope(
                eventId,
                EventEnvelope.TYPE_RISK_CALCULATED,
                1,
                Instant.now(),
                "risk-service",
                UUID.randomUUID(),
                payload);
        return objectMapper.writeValueAsString(envelope);
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
