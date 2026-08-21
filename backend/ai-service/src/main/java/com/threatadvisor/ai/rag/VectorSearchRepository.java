package com.threatadvisor.ai.rag;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.threatadvisor.ai.domain.KnowledgeChunkHit;

import java.util.List;
import java.util.UUID;

@Repository
public class VectorSearchRepository {

    private final JdbcTemplate jdbc;

    public VectorSearchRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void replaceDocumentChunks(UUID documentId, List<ChunkInsert> chunks) {
        jdbc.update("DELETE FROM knowledge_chunks WHERE document_id = ?", documentId);
        for (ChunkInsert chunk : chunks) {
            jdbc.update(
                    """
                    INSERT INTO knowledge_chunks (id, document_id, chunk_index, content, metadata, embedding)
                    VALUES (?, ?, ?, ?, CAST(? AS jsonb), CAST(? AS vector))
                    """,
                    chunk.id(),
                    documentId,
                    chunk.index(),
                    chunk.content(),
                    chunk.metadataJson(),
                    chunk.embeddingLiteral());
        }
    }

    public List<KnowledgeChunkHit> search(String embeddingLiteral, int topK) {
        return jdbc.query(
                """
                SELECT c.id, c.document_id, d.title, d.source, d.document_type, c.content,
                       (c.embedding <=> CAST(? AS vector)) AS distance
                FROM knowledge_chunks c
                JOIN knowledge_documents d ON d.id = c.document_id
                WHERE c.embedding IS NOT NULL
                ORDER BY c.embedding <=> CAST(? AS vector)
                LIMIT ?
                """,
                (rs, rowNum) -> new KnowledgeChunkHit(
                        rs.getObject("id", UUID.class),
                        rs.getObject("document_id", UUID.class),
                        rs.getString("title"),
                        rs.getString("source"),
                        rs.getString("document_type"),
                        rs.getString("content"),
                        rs.getDouble("distance")),
                embeddingLiteral,
                embeddingLiteral,
                topK);
    }

    public record ChunkInsert(UUID id, int index, String content, String metadataJson, String embeddingLiteral) {
    }
}
