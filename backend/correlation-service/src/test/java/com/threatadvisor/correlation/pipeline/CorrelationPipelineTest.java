package com.threatadvisor.correlation.pipeline;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.threatadvisor.correlation.domain.Asset;
import com.threatadvisor.correlation.domain.AssetSoftware;
import com.threatadvisor.correlation.domain.Finding;
import com.threatadvisor.correlation.domain.Organization;
import com.threatadvisor.correlation.domain.Vulnerability;
import com.threatadvisor.correlation.domain.VulnerabilityCpe;
import com.threatadvisor.correlation.kafka.EventEnvelope;
import com.threatadvisor.correlation.repository.AssetRepository;
import com.threatadvisor.correlation.repository.AssetSoftwareRepository;
import com.threatadvisor.correlation.repository.EventProcessingRecordRepository;
import com.threatadvisor.correlation.repository.FindingRepository;
import com.threatadvisor.correlation.repository.OrganizationRepository;
import com.threatadvisor.correlation.repository.VulnerabilityCpeRepository;
import com.threatadvisor.correlation.repository.VulnerabilityRepository;
import com.threatadvisor.correlation.service.CorrelationService;
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

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers(disabledWithoutDocker = true)
@DisabledIfEnvironmentVariable(named = "SKIP_TESTCONTAINERS", matches = "true")
class CorrelationPipelineTest {

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
    private CorrelationService correlationService;
    @Autowired
    private OrganizationRepository organizations;
    @Autowired
    private AssetRepository assets;
    @Autowired
    private AssetSoftwareRepository software;
    @Autowired
    private VulnerabilityRepository vulnerabilities;
    @Autowired
    private VulnerabilityCpeRepository cpes;
    @Autowired
    private FindingRepository findings;
    @Autowired
    private EventProcessingRecordRepository processingRecords;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    private UUID orgId;
    private UUID httpdVulnId;
    private UUID opensslVulnId;
    private UUID unusedVulnId;
    private UUID exactVulnId;
    private UUID vulnHttpdAsset;
    private UUID patchedHttpdAsset;
    private UUID nginxAsset;
    private UUID opensslA;
    private UUID opensslB;
    private UUID belowRangeAsset;
    private UUID aboveRangeAsset;

    @BeforeEach
    void seed() {
        findings.deleteAll();
        processingRecords.deleteAll();
        software.deleteAll();
        cpes.deleteAll();
        vulnerabilities.deleteAll();
        assets.deleteAll();
        organizations.deleteAll();

        Instant now = Instant.now();
        orgId = UUID.randomUUID();
        Organization org = new Organization();
        org.setId(orgId);
        org.setName("Northwind");
        org.setSlug("northwind-test");
        org.setCreatedAt(now);
        org.setUpdatedAt(now);
        organizations.save(org);

        vulnHttpdAsset = asset("web-prod-01", true);
        patchedHttpdAsset = asset("web-prod-patched", true);
        nginxAsset = asset("edge-nginx-01", true);
        opensslA = asset("nw-int-jump-01", false);
        opensslB = asset("nw-edge-vpn-01", true);
        belowRangeAsset = asset("nw-prod-httpd-old-01", true);
        aboveRangeAsset = asset("nw-stg-web-01", true);
        asset("unused-host", false);

        software(vulnHttpdAsset, "apache", "http_server", "2.4.49");
        software(patchedHttpdAsset, "apache", "http_server", "2.4.51");
        software(nginxAsset, "nginx", "nginx", "1.24.0");
        software(opensslA, "openssl", "openssl", "3.0.2");
        software(opensslB, "openssl", "openssl", "3.0.2");
        software(belowRangeAsset, "apache", "http_server", "2.3.9");
        software(aboveRangeAsset, "apache", "http_server", "2.4.57");

        exactVulnId = vulnerability("CVE-2025-1234", "CRITICAL");
        cpe(exactVulnId, "cpe:2.3:a:apache:http_server:2.4.49:*:*:*:*:*:*:*", "apache", "http_server", null, null);

        httpdVulnId = vulnerability("CVE-2023-25690", "CRITICAL");
        cpe(httpdVulnId, "cpe:2.3:a:apache:http_server:*:*:*:*:*:*:*:*", "apache", "http_server", "2.4.0", "2.4.56");

        opensslVulnId = vulnerability("CVE-2022-3602", "HIGH");
        cpe(opensslVulnId, "cpe:2.3:a:openssl:openssl:*:*:*:*:*:*:*:*", "openssl", "openssl", "3.0.0", "3.0.7");

        unusedVulnId = vulnerability("CVE-2023-38408", "CRITICAL");
        cpe(unusedVulnId, "cpe:2.3:a:openbsd:openssh:*:*:*:*:*:*:*:*", "openbsd", "openssh", "8.9", "9.3.2");
    }

    @Test
    void correlatesVulnerableExactVersionAndSkipsOthers() throws Exception {
        CorrelationService.CorrelationOutcome first = correlationService.handleNormalizedEvent(
                envelope(exactVulnId, "CVE-2025-1234"));
        assertEquals("COMPLETED", first.status());
        assertEquals(1, findings.countByVulnerabilityId(exactVulnId));
        Finding finding = findings.findByVulnerabilityId(exactVulnId).get(0);
        assertEquals(vulnHttpdAsset, finding.getAssetId());
        assertEquals("EXACT_VERSION_MATCH", finding.getMatchType());
        assertEquals("HIGH", finding.getMatchConfidence());
        assertTrue(finding.getMatchExplanation().contains("web-prod-01"));
        assertTrue(finding.getMatchExplanation().contains("2.4.49"));

        CorrelationService.CorrelationOutcome duplicate = correlationService.handleNormalizedEvent(
                envelope(exactVulnId, "CVE-2025-1234", first.eventId()));
        assertEquals("DUPLICATE", duplicate.status());
        assertEquals(1, findings.countByVulnerabilityId(exactVulnId));
        assertTrue(processingRecords.existsByEventIdAndConsumer(first.eventId(), CorrelationService.CONSUMER));
    }

    @Test
    void rangeMatchOnlyInsideWindowWrongProductAndMissingSoftware() throws Exception {
        correlationService.handleNormalizedEvent(envelope(httpdVulnId, "CVE-2023-25690"));
        List<Finding> httpd = findings.findByVulnerabilityId(httpdVulnId);
        assertEquals(1, httpd.size());
        assertEquals(vulnHttpdAsset, httpd.get(0).getAssetId());
        assertEquals("VERSION_RANGE_MATCH", httpd.get(0).getMatchType());
        assertTrue(httpd.get(0).getMatchExplanation().contains(">= 2.4.0"));
        assertTrue(httpd.stream().noneMatch(f -> f.getAssetId().equals(nginxAsset)));
        assertTrue(httpd.stream().noneMatch(f -> f.getAssetId().equals(belowRangeAsset)));
        assertTrue(httpd.stream().noneMatch(f -> f.getAssetId().equals(aboveRangeAsset)));
        assertTrue(httpd.stream().noneMatch(f -> f.getAssetId().equals(patchedHttpdAsset)));

        correlationService.handleNormalizedEvent(envelope(opensslVulnId, "CVE-2022-3602"));
        assertEquals(2, findings.countByVulnerabilityId(opensslVulnId));

        correlationService.handleNormalizedEvent(envelope(unusedVulnId, "CVE-2023-38408"));
        assertEquals(0, findings.countByVulnerabilityId(unusedVulnId));
    }

    @Test
    void kafkaConsumerCreatesFinding() throws Exception {
        UUID eventId = UUID.randomUUID();
        kafkaTemplate.send(EventEnvelope.TYPE_NORMALIZED, "CVE-2025-1234", envelope(exactVulnId, "CVE-2025-1234", eventId)).get();
        await().atMost(java.time.Duration.ofSeconds(30)).untilAsserted(() -> {
            assertEquals(1, findings.countByVulnerabilityId(exactVulnId));
            assertTrue(processingRecords.existsByEventIdAndConsumer(eventId, CorrelationService.CONSUMER));
        });
    }

    private String envelope(UUID vulnerabilityId, String cveId) throws Exception {
        return envelope(vulnerabilityId, cveId, UUID.randomUUID());
    }

    private String envelope(UUID vulnerabilityId, String cveId, UUID eventId) throws Exception {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("vulnerabilityId", vulnerabilityId.toString());
        payload.put("cveId", cveId);
        EventEnvelope envelope = new EventEnvelope(
                eventId,
                EventEnvelope.TYPE_NORMALIZED,
                1,
                Instant.now(),
                "ingestion-service",
                UUID.randomUUID(),
                payload);
        return objectMapper.writeValueAsString(envelope);
    }

    private UUID asset(String hostname, boolean internet) {
        Instant now = Instant.now();
        Asset asset = new Asset();
        asset.setId(UUID.randomUUID());
        asset.setOrganizationId(orgId);
        asset.setHostname(hostname);
        asset.setEnvironment("PRODUCTION");
        asset.setBusinessCriticality("HIGH");
        asset.setInternetExposure(internet);
        asset.setStatus("ACTIVE");
        asset.setCreatedAt(now);
        asset.setUpdatedAt(now);
        return assets.save(asset).getId();
    }

    private void software(UUID assetId, String vendor, String product, String version) {
        AssetSoftware row = new AssetSoftware();
        row.setId(UUID.randomUUID());
        row.setAssetId(assetId);
        row.setVendor(vendor);
        row.setProduct(product);
        row.setVersion(version);
        row.setInstallationStatus("INSTALLED");
        software.save(row);
    }

    private UUID vulnerability(String cveId, String severity) {
        Instant now = Instant.now();
        Vulnerability vuln = new Vulnerability();
        vuln.setId(UUID.randomUUID());
        vuln.setCveId(cveId);
        vuln.setSeverity(severity);
        vuln.setMetadata("{}");
        vuln.setCvssMetrics("{}");
        vuln.setCreatedAt(now);
        vuln.setUpdatedAt(now);
        return vulnerabilities.save(vuln).getId();
    }

    private void cpe(UUID vulnerabilityId, String cpe, String vendor, String product, String startInc, String endExc) {
        VulnerabilityCpe row = new VulnerabilityCpe();
        row.setId(UUID.randomUUID());
        row.setVulnerabilityId(vulnerabilityId);
        row.setCpe(cpe);
        row.setVendor(vendor);
        row.setProduct(product);
        row.setVersionStartIncluding(startInc);
        row.setVersionEndExcluding(endExc);
        cpes.save(row);
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
