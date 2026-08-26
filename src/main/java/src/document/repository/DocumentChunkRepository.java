package src.document.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import src.document.dto.DocumentChunkSearchResult;
import src.entity.Document;
import src.entity.DocumentChunk;

import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentChunkRepository
        extends JpaRepository<DocumentChunk, UUID> {

    List<DocumentChunk> findByDocumentOrderByChunkIndexAsc(
            Document document
    );

    void deleteByDocument(Document document);

    boolean existsByDocument(Document document);

    long countByDocument(Document document);

    /**
     * Searches all READY documents within a workspace.
     */
    @Query(
            value = """
                    SELECT
                        dc.id AS chunkId,
                        dc.document_id AS documentId,
                        d.title AS documentTitle,
                        dc.chunk_index AS chunkIndex,
                        dc.content AS content,
                        CAST(
                            dc.embedding <=> CAST(:queryEmbedding AS vector)
                            AS double precision
                        ) AS distance
                    FROM document_chunks dc
                    JOIN documents d
                        ON d.id = dc.document_id
                    WHERE d.workspace_id = :workspaceId
                      AND d.upload_status = 'READY'
                      AND dc.embedding IS NOT NULL
                    ORDER BY
                        dc.embedding <=> CAST(:queryEmbedding AS vector)
                    LIMIT :resultLimit
                    """,
            nativeQuery = true
    )
    List<DocumentChunkSearchResult> findSimilarChunks(
            @Param("workspaceId") UUID workspaceId,
            @Param("queryEmbedding") String queryEmbedding,
            @Param("resultLimit") int resultLimit
    );

    /**
     * Searches only selected READY documents within a workspace.
     */
    @Query(
            value = """
                    SELECT
                        dc.id AS chunkId,
                        dc.document_id AS documentId,
                        d.title AS documentTitle,
                        dc.chunk_index AS chunkIndex,
                        dc.content AS content,
                        CAST(
                            dc.embedding <=> CAST(:queryEmbedding AS vector)
                            AS double precision
                        ) AS distance
                    FROM document_chunks dc
                    JOIN documents d
                        ON d.id = dc.document_id
                    WHERE d.workspace_id = :workspaceId
                      AND d.upload_status = 'READY'
                      AND dc.embedding IS NOT NULL
                      AND dc.document_id IN (:documentIds)
                    ORDER BY
                        dc.embedding <=> CAST(:queryEmbedding AS vector)
                    LIMIT :resultLimit
                    """,
            nativeQuery = true
    )
    List<DocumentChunkSearchResult> findSimilarChunksInDocuments(
            @Param("workspaceId") UUID workspaceId,
            @Param("documentIds") List<UUID> documentIds,
            @Param("queryEmbedding") String queryEmbedding,
            @Param("resultLimit") int resultLimit
    );

    /**
     * Loads document chunks in their original document order.
     *
     * Used for broad questions where complete document coverage
     * is more important than semantic similarity.
     */
    @Query(
            value = """
                SELECT
                    dc.id AS chunkId,
                    dc.document_id AS documentId,
                    d.title AS documentTitle,
                    dc.chunk_index AS chunkIndex,
                    dc.content AS content,
                    CAST(NULL AS double precision) AS distance
                FROM document_chunks dc
                JOIN documents d
                    ON d.id = dc.document_id
                WHERE d.workspace_id = :workspaceId
                  AND d.upload_status = 'READY'
                  AND dc.document_id IN (:documentIds)
                ORDER BY
                    dc.document_id,
                    dc.chunk_index
                LIMIT :resultLimit
                """,
            nativeQuery = true
    )
    List<DocumentChunkSearchResult> findDocumentContextChunks(
            @Param("workspaceId") UUID workspaceId,
            @Param("documentIds") List<UUID> documentIds,
            @Param("resultLimit") int resultLimit
    );
}