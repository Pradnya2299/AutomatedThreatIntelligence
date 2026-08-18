package com.threatadvisor.api.infrastructure;

import io.lettuce.core.RedisClient;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 2A: infrastructure foundation only. Does not run CVE correlation, risk, or AI.
 */
@Testcontainers
@DisabledIfEnvironmentVariable(named = "SKIP_TESTCONTAINERS", matches = "true")
class InfrastructureFoundationTest {

    private static final DockerImageName PGVECTOR =
            DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres");

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(PGVECTOR)
            .withDatabaseName("threat_advisor")
            .withUsername("threat_advisor")
            .withPassword("threat_advisor_dev_change_me");

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @Container
    static final KafkaContainer KAFKA = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));

    @Test
    void postgresFlywayPgvectorAndSeed() throws Exception {
        Path repoRoot = findRepoRoot();
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("filesystem:" + repoRoot.resolve("database/migrations").toAbsolutePath())
                .load()
                .migrate();

        try (Connection conn = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())) {
            try (ResultSet rs = conn.createStatement()
                    .executeQuery("SELECT extname FROM pg_extension WHERE extname = 'vector'")) {
                assertTrue(rs.next(), "pgvector extension must exist");
            }

            String seedSql = Files.readString(repoRoot.resolve("database/seed/demo_seed.sql"));
            conn.createStatement().execute(seedSql);

            try (ResultSet rs = conn.createStatement().executeQuery("SELECT COUNT(*) FROM assets")) {
                assertTrue(rs.next());
                assertTrue(rs.getInt(1) >= 20, "expected 20+ seeded assets");
            }
            try (ResultSet rs = conn.createStatement().executeQuery("SELECT COUNT(*) FROM vulnerabilities")) {
                assertTrue(rs.next());
                assertTrue(rs.getInt(1) >= 5, "expected demo CVEs");
            }
            try (ResultSet rs = conn.createStatement().executeQuery(
                    """
                    SELECT COUNT(*) FROM asset_software s
                    JOIN assets a ON a.id = s.asset_id
                    WHERE a.hostname = 'nw-prod-edge-gw-01'
                      AND s.product = 'log4j'
                      AND s.version = '2.14.1'
                      AND a.internet_exposure = TRUE
                    """)) {
                assertTrue(rs.next());
                assertEquals(1, rs.getInt(1), "scenario A inventory must exist");
            }
            try (ResultSet rs = conn.createStatement().executeQuery(
                    "SELECT COUNT(*) FROM knowledge_chunks WHERE embedding IS NULL")) {
                assertTrue(rs.next());
                assertTrue(rs.getInt(1) >= 1, "embeddings remain unset in Phase 2A");
            }
        }
    }

    @Test
    void redisRespondsToPing() {
        String uri = "redis://" + REDIS.getHost() + ":" + REDIS.getMappedPort(6379);
        try (RedisClient client = RedisClient.create(uri);
             var connection = client.connect()) {
            assertEquals("PONG", connection.sync().ping());
        }
    }

    @Test
    void kafkaAcceptsTopicManagement() throws Exception {
        try (AdminClient admin = AdminClient.create(Map.of(
                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers(),
                AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, "15000"))) {
            admin.createTopics(List.of(new NewTopic("cve.raw", 1, (short) 1)))
                    .all()
                    .get(30, TimeUnit.SECONDS);
            Set<String> topics = admin.listTopics().names().get(30, TimeUnit.SECONDS);
            assertTrue(topics.contains("cve.raw"));
        }
    }

    private static Path findRepoRoot() {
        Path current = Path.of("").toAbsolutePath();
        for (int i = 0; i < 6; i++) {
            if (Files.isDirectory(current.resolve("database/migrations"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Could not locate database/migrations from " + Path.of("").toAbsolutePath());
    }
}
