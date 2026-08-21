package com.threatadvisor.ai.rag;

import com.threatadvisor.ai.config.AiProperties;
import com.threatadvisor.ai.domain.KnowledgeDocument;
import com.threatadvisor.ai.repository.KnowledgeDocumentRepository;
import com.threatadvisor.ai.repository.OrganizationRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KnowledgeIngestionServiceTest {

    @Test
    void ingestOnStartupDoesNotAbortWhenEmbeddingsFail() {
        AiProperties properties = new AiProperties();
        properties.setIngestOnStartup(true);
        properties.setChunkSizeChars(2000);
        properties.setChunkOverlapChars(200);

        EmbeddingService embeddings = mock(EmbeddingService.class);
        when(embeddings.embed(any())).thenThrow(new RuntimeException("OpenAI 401"));

        KnowledgeDocumentRepository documents = mock(KnowledgeDocumentRepository.class);
        when(documents.findBySource(any())).thenReturn(Optional.empty());
        when(documents.saveAndFlush(any(KnowledgeDocument.class))).thenAnswer(invocation -> {
            KnowledgeDocument document = invocation.getArgument(0);
            if (document.getId() == null) {
                document.setId(UUID.randomUUID());
            }
            return document;
        });

        OrganizationRepository organizations = mock(OrganizationRepository.class);
        when(organizations.findBySlug("northwind")).thenReturn(Optional.empty());

        KnowledgeIngestionService service = new KnowledgeIngestionService(
                properties,
                embeddings,
                documents,
                organizations,
                mock(VectorSearchRepository.class));

        assertDoesNotThrow(service::ingestOnStartup);
        assertThrows(IllegalStateException.class, service::ingestClasspath);
    }
}
