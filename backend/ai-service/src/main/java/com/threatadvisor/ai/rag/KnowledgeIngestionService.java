package com.threatadvisor.ai.rag;

import com.threatadvisor.ai.config.AiProperties;
import com.threatadvisor.ai.domain.KnowledgeDocument;
import com.threatadvisor.ai.domain.Organization;
import com.threatadvisor.ai.repository.KnowledgeDocumentRepository;
import com.threatadvisor.ai.repository.OrganizationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
public class KnowledgeIngestionService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeIngestionService.class);

    private final AiProperties properties;
    private final EmbeddingService embeddings;
    private final KnowledgeDocumentRepository documents;
    private final OrganizationRepository organizations;
    private final VectorSearchRepository vectors;

    public KnowledgeIngestionService(
            AiProperties properties,
            EmbeddingService embeddings,
            KnowledgeDocumentRepository documents,
            OrganizationRepository organizations,
            VectorSearchRepository vectors) {
        this.properties = properties;
        this.embeddings = embeddings;
        this.documents = documents;
        this.organizations = organizations;
        this.vectors = vectors;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void ingestOnStartup() {
        if (!properties.isIngestOnStartup()) {
            return;
        }
        try {
            ingestClasspath();
        } catch (RuntimeException ex) {
            // ApplicationReadyEvent exceptions abort Spring Boot (Maven spring-boot:run
            // then reports exit code 1). RAG is optional for LLM code remediation.
            log.error(
                    "operation=knowledge.ingest.startup-failed ai-service will keep running; "
                            + "RAG may be empty until POST /internal/ai/knowledge/ingest succeeds",
                    ex);
        }
    }

    @Transactional
    public int ingestClasspath() {
        try {
            Resource[] resources = new PathMatchingResourcePatternResolver().getResources("classpath:knowledge/*.md");
            int count = 0;
            Optional<Organization> org = organizations.findBySlug("northwind");
            for (Resource resource : resources) {
                String filename = resource.getFilename();
                if (filename == null) {
                    continue;
                }
                String body = resource.getContentAsString(StandardCharsets.UTF_8);
                upsert(org.map(Organization::getId).orElse(null), filename, body);
                count++;
            }
            log.info("operation=knowledge.ingest documents={}", count);
            return count;
        } catch (Exception ex) {
            log.error("operation=knowledge.ingest.failed", ex);
            throw new IllegalStateException("Knowledge ingest failed", ex);
        }
    }

    private void upsert(UUID organizationId, String filename, String body) {
        String source = "classpath:knowledge/" + filename;
        Instant now = Instant.now();
        KnowledgeDocument document = documents.findBySource(source).orElseGet(KnowledgeDocument::new);
        if (document.getId() == null) {
            document.setId(UUID.randomUUID());
            document.setCreatedAt(now);
        }
        document.setOrganizationId(organizationId);
        document.setTitle(titleFrom(body, filename));
        document.setDocumentType(typeFrom(filename));
        document.setSource(source);
        document.setBody(body);
        document.setMetadata("{\"filename\":\"" + filename + "\"}");
        document.setUpdatedAt(now);
        documents.saveAndFlush(document);

        List<String> parts = TextChunker.chunk(body, properties.getChunkSizeChars(), properties.getChunkOverlapChars());
        List<VectorSearchRepository.ChunkInsert> inserts = new ArrayList<>();
        for (int i = 0; i < parts.size(); i++) {
            inserts.add(new VectorSearchRepository.ChunkInsert(
                    UUID.randomUUID(),
                    i,
                    parts.get(i),
                    "{\"source\":\"" + source + "\"}",
                    embeddings.embed(parts.get(i))));
        }
        vectors.replaceDocumentChunks(document.getId(), inserts);
    }

    static String titleFrom(String body, String filename) {
        for (String line : body.split("\n")) {
            if (line.startsWith("# ")) {
                return line.substring(2).trim();
            }
        }
        return filename;
    }

    static String typeFrom(String filename) {
        return filename.replace(".md", "").replace('-', '_').toUpperCase(Locale.ROOT);
    }
}
