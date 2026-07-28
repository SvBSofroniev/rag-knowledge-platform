package src.rag.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import src.common.exception.AiModelResponseException;
import src.common.exception.BadRequestException;
import src.document.dto.DocumentChunkSearchResult;
import src.document.repository.DocumentChunkRepository;
import src.embedding.service.EmbeddingService;
import src.entity.User;
import src.rag.dto.SemanticSearchResponse;
import src.workspace.service.WorkspacePermissionService;

import java.util.List;
import java.util.Objects;
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
        String normalizedQuery =
                validateAndNormalizeQuery(query);

        int normalizedLimit =
                validateAndNormalizeLimit(limit);

        /*
         * Prevent retrieval from workspaces to which the
         * authenticated user does not belong.
         */
        permissionService.requireMember(
                workspaceId,
                currentUser
        );

        float[] queryEmbedding =
                embeddingService.generateEmbedding(
                        normalizedQuery
                );

        validateEmbedding(queryEmbedding);

        String vectorLiteral =
                toVectorLiteral(queryEmbedding);

        List<DocumentChunkSearchResult> databaseResults =
                chunkRepository.findSimilarChunks(
                        workspaceId,
                        vectorLiteral,
                        normalizedLimit
                );

        return mapLogAndFilterResults(
                workspaceId,
                normalizedQuery,
                databaseResults
        );
    }

    public List<SemanticSearchResponse> searchInDocuments(
            UUID workspaceId,
            List<UUID> documentIds,
            String query,
            Integer limit,
            User currentUser
    ) {
        String normalizedQuery =
                validateAndNormalizeQuery(query);

        int normalizedLimit =
                validateAndNormalizeLimit(limit);

        /*
         * The repository query is scoped by workspaceId, but we
         * must also verify that the current user is a member.
         */
        permissionService.requireMember(
                workspaceId,
                currentUser
        );

        List<UUID> uniqueDocumentIds =
                validateAndNormalizeDocumentIds(
                        documentIds
                );

        float[] queryEmbedding =
                embeddingService.generateEmbedding(
                        normalizedQuery
                );

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

        return mapLogAndFilterResults(
                workspaceId,
                normalizedQuery,
                databaseResults
        );
    }

    private String validateAndNormalizeQuery(
            String query
    ) {
        if (query == null || query.isBlank()) {
            throw new BadRequestException(
                    "Search query cannot be empty"
            );
        }

        String normalizedQuery = query.trim();

        if (normalizedQuery.length() > MAX_QUERY_LENGTH) {
            throw new BadRequestException(
                    "Search query cannot exceed " +
                            MAX_QUERY_LENGTH +
                            " characters"
            );
        }

        return normalizedQuery;
    }

    private int validateAndNormalizeLimit(
            Integer limit
    ) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }

        if (limit < 1) {
            throw new BadRequestException(
                    "Search result limit must be at least 1"
            );
        }

        if (limit > MAX_LIMIT) {
            throw new BadRequestException(
                    "Search result limit cannot exceed " +
                            MAX_LIMIT
            );
        }

        return limit;
    }

    private List<UUID> validateAndNormalizeDocumentIds(
            List<UUID> documentIds
    ) {
        if (documentIds == null || documentIds.isEmpty()) {
            throw new BadRequestException(
                    "At least one document must be provided"
            );
        }

        List<UUID> uniqueDocumentIds = documentIds
                .stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (uniqueDocumentIds.isEmpty()) {
            throw new BadRequestException(
                    "At least one valid document must be provided"
            );
        }

        return uniqueDocumentIds;
    }

    private void validateEmbedding(
            float[] embedding
    ) {
        if (embedding == null) {
            throw new AiModelResponseException(
                    "The embedding model returned no vector"
            );
        }

        if (embedding.length != EXPECTED_DIMENSIONS) {
            throw new AiModelResponseException(
                    "Expected a " +
                            EXPECTED_DIMENSIONS +
                            "-dimensional embedding, but received " +
                            embedding.length
            );
        }

        for (float value : embedding) {
            if (!Float.isFinite(value)) {
                throw new AiModelResponseException(
                        "The embedding model returned an invalid vector"
                );
            }
        }
    }

    private List<SemanticSearchResponse> mapLogAndFilterResults(
            UUID workspaceId,
            String query,
            List<DocumentChunkSearchResult> databaseResults
    ) {
        if (databaseResults == null ||
                databaseResults.isEmpty()) {
            logSearchResults(
                    workspaceId,
                    query,
                    List.of()
            );

            return List.of();
        }

        List<SemanticSearchResponse> mappedResults =
                databaseResults.stream()
                        .map(this::toResponse)
                        .toList();

        /*
         * Log before filtering so rejected results can still be
         * inspected while calibrating the similarity threshold.
         */
        logSearchResults(
                workspaceId,
                query,
                mappedResults
        );

        return mappedResults.stream()
                .filter(this::meetsSimilarityThreshold)
                .toList();
    }

    private SemanticSearchResponse toResponse(
            DocumentChunkSearchResult result
    ) {
        if (result == null) {
            throw new AiModelResponseException(
                    "Semantic search returned an invalid result"
            );
        }

        Double distance = result.getDistance();

        if (distance == null ||
                !Double.isFinite(distance)) {
            throw new AiModelResponseException(
                    "Semantic search returned an invalid distance"
            );
        }

        /*
         * For cosine distance:
         *
         * similarity = 1 - distance
         *
         * Cosine similarity can range from -1 to 1, so it should
         * not be forcibly clamped to the 0–1 range.
         */
        double similarity = 1.0 - distance;

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
        return result.similarity() != null &&
                Double.isFinite(result.similarity()) &&
                result.similarity() >= minimumSimilarity;
    }

    private String toVectorLiteral(
            float[] embedding
    ) {
        StringBuilder vector =
                new StringBuilder(embedding.length * 12);

        vector.append('[');

        for (int index = 0;
             index < embedding.length;
             index++) {

            if (index > 0) {
                vector.append(',');
            }

            vector.append(
                    Float.toString(embedding[index])
            );
        }

        vector.append(']');

        return vector.toString();
    }

    private void logSearchResults(
            UUID workspaceId,
            String query,
            List<SemanticSearchResponse> results
    ) {
        /*
         * Debug level avoids filling production logs with user
         * questions and document retrieval details.
         */
        log.debug(
                "Semantic search completed: workspace={}, queryLength={}, threshold={}, results={}",
                workspaceId,
                query.length(),
                minimumSimilarity,
                results.size()
        );

        for (SemanticSearchResponse result : results) {
            log.debug(
                    "Semantic result: document={}, chunk={}, similarity={}, distance={}, accepted={}",
                    result.documentTitle(),
                    result.chunkIndex(),
                    result.similarity(),
                    result.distance(),
                    meetsSimilarityThreshold(result)
            );
        }
    }
}