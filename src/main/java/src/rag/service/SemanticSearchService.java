package src.rag.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import src.document.dto.DocumentChunkSearchResult;
import src.document.repository.DocumentChunkRepository;
import src.embedding.service.EmbeddingService;
import src.entity.User;
import src.rag.dto.SemanticSearchResponse;
import src.workspace.service.WorkspacePermissionService;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SemanticSearchService {

    private static final int EXPECTED_DIMENSIONS = 768;
    private static final int DEFAULT_LIMIT = 5;
    private static final int MAX_LIMIT = 20;
    private static final int MAX_QUERY_LENGTH = 5_000;

    private final EmbeddingService embeddingService;
    private final DocumentChunkRepository chunkRepository;
    private final WorkspacePermissionService permissionService;

    @Value("${rag.search.minimum-similarity:0.35}")
    private double minimumSimilarity;

    public List<SemanticSearchResponse> search(
            UUID workspaceId,
            String query,
            Integer limit,
            User currentUser
    ) {
        validateQuery(query);

        // Ensures the user cannot retrieve chunks from another workspace.
        permissionService.requireMember(
                workspaceId,
                currentUser
        );

        String normalizedQuery = query.trim();
        int normalizedLimit = normalizeLimit(limit);

        float[] queryEmbedding =
                embeddingService.generateEmbedding(normalizedQuery);

        validateEmbedding(queryEmbedding);

        String vectorLiteral =
                toVectorLiteral(queryEmbedding);

        List<DocumentChunkSearchResult> databaseResults =
                chunkRepository.findSimilarChunks(
                        workspaceId,
                        vectorLiteral,
                        normalizedLimit
                );

        List<SemanticSearchResponse> mappedResults =
                databaseResults.stream()
                        .map(this::toResponse)
                        .toList();

        logSearchResults(
                workspaceId,
                normalizedQuery,
                mappedResults
        );

        return mappedResults.stream()
                .filter(this::meetsSimilarityThreshold)
                .toList();
    }

    private SemanticSearchResponse toResponse(
            DocumentChunkSearchResult result
    ) {
        Double distance = result.getDistance();

        Double similarity = distance == null
                ? null
                : 1.0 - distance;

        return new SemanticSearchResponse(
                result.getChunkId(),
                result.getDocumentId(),
                result.getDocumentTitle(),
                result.getChunkIndex(),
                result.getContent(),
                distance,
                similarity
        );
    }

    private boolean meetsSimilarityThreshold(
            SemanticSearchResponse result
    ) {
        return result.similarity() != null
                && result.similarity() >= minimumSimilarity;
    }

    private void validateQuery(String query) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException(
                    "Search query cannot be empty"
            );
        }

        if (query.length() > MAX_QUERY_LENGTH) {
            throw new IllegalArgumentException(
                    "Search query cannot exceed " +
                            MAX_QUERY_LENGTH +
                            " characters"
            );
        }
    }

    private void validateEmbedding(float[] embedding) {
        if (embedding == null) {
            throw new RuntimeException(
                    "Embedding model returned null"
            );
        }

        if (embedding.length != EXPECTED_DIMENSIONS) {
            throw new RuntimeException(
                    "Expected a " +
                            EXPECTED_DIMENSIONS +
                            "-dimensional embedding, but received " +
                            embedding.length
            );
        }

        for (float value : embedding) {
            if (!Float.isFinite(value)) {
                throw new RuntimeException(
                        "Embedding contains a non-finite value"
                );
            }
        }
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }

        if (limit < 1) {
            throw new IllegalArgumentException(
                    "Search result limit must be at least 1"
            );
        }

        return Math.min(limit, MAX_LIMIT);
    }

    private String toVectorLiteral(float[] embedding) {
        StringBuilder vector =
                new StringBuilder(embedding.length * 12);

        vector.append('[');

        for (int index = 0; index < embedding.length; index++) {
            if (index > 0) {
                vector.append(',');
            }

            vector.append(Float.toString(embedding[index]));
        }

        vector.append(']');

        return vector.toString();
    }

    private void logSearchResults(
            UUID workspaceId,
            String query,
            List<SemanticSearchResponse> results
    ) {
        log.info(
                "Semantic search: workspace={}, query=\"{}\", threshold={}, results={}",
                workspaceId,
                query,
                minimumSimilarity,
                results.size()
        );

        for (SemanticSearchResponse result : results) {
            log.info(
                    "Semantic result: document={}, chunk={}, similarity={}, distance={}, accepted={}",
                    result.documentTitle(),
                    result.chunkIndex(),
                    result.similarity(),
                    result.distance(),
                    meetsSimilarityThreshold(result)
            );
        }
    }

    public List<SemanticSearchResponse> searchInDocuments(
            UUID workspaceId,
            List<UUID> documentIds,
            String query,
            Integer limit,
            User currentUser
    ) {
        validateQuery(query);

        permissionService.requireMember(
                workspaceId,
                currentUser
        );

        if (documentIds == null || documentIds.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one document must be provided"
            );
        }

        List<UUID> uniqueDocumentIds = documentIds.stream()
                .filter(documentId -> documentId != null)
                .distinct()
                .toList();

        if (uniqueDocumentIds.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one valid document must be provided"
            );
        }

        String normalizedQuery = query.trim();
        int normalizedLimit = normalizeLimit(limit);

        float[] queryEmbedding =
                embeddingService.generateEmbedding(normalizedQuery);

        validateEmbedding(queryEmbedding);

        String vectorLiteral =
                toVectorLiteral(queryEmbedding);

        List<DocumentChunkSearchResult> databaseResults =
                chunkRepository.findSimilarChunksInDocuments(
                        workspaceId,
                        uniqueDocumentIds,
                        vectorLiteral,
                        normalizedLimit
                );

        List<SemanticSearchResponse> mappedResults =
                databaseResults.stream()
                        .map(this::toResponse)
                        .toList();

        logSearchResults(
                workspaceId,
                normalizedQuery,
                mappedResults
        );

        return mappedResults.stream()
                .filter(this::meetsSimilarityThreshold)
                .toList();
    }
}