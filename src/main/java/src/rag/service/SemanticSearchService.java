package src.rag.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import src.common.exception.AiModelResponseException;
import src.common.exception.ApiErrorCodes;
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
    private static final int MAX_CONTEXT_LIMIT = 100;

    private final EmbeddingService embeddingService;
    private final DocumentChunkRepository chunkRepository;
    private final WorkspacePermissionService permissionService;

    @Value("${rag.search.minimum-similarity:0.35}")
    private double minimumSimilarity;

    @Value("${rag.search.fallback-minimum-similarity:0.20}")
    private double fallbackMinimumSimilarity;

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
                databaseResults,
                false
        );
    }

    public List<SemanticSearchResponse> searchInDocuments(
            UUID workspaceId,
            List<UUID> documentIds,
            String query,
            Integer limit,
            User currentUser
    ) {
        return searchInDocumentsInternal(
                workspaceId,
                documentIds,
                query,
                limit,
                currentUser,
                false
        );
    }

    public List<SemanticSearchResponse> searchInDocumentsWithFallback(
            UUID workspaceId,
            List<UUID> documentIds,
            String query,
            Integer limit,
            User currentUser
    ) {
        return searchInDocumentsInternal(
                workspaceId,
                documentIds,
                query,
                limit,
                currentUser,
                true
        );
    }

    public List<SemanticSearchResponse> getDocumentContext(
            UUID workspaceId,
            List<UUID> documentIds,
            Integer limit,
            User currentUser
    ) {
        permissionService.requireMember(
                workspaceId,
                currentUser
        );

        List<UUID> uniqueDocumentIds =
                validateAndNormalizeDocumentIds(
                        documentIds
                );

        int normalizedLimit =
                validateContextLimit(
                        limit
                );

        List<DocumentChunkSearchResult> results =
                chunkRepository.findDocumentContextChunks(
                        workspaceId,
                        uniqueDocumentIds,
                        normalizedLimit
                );

        if (results == null ||
                results.isEmpty()) {
            return List.of();
        }

        return results.stream()
                .map(this::toContextResponse)
                .toList();
    }

    private int validateContextLimit(
            Integer limit
    ) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }

        if (limit < 1) {
            throw new BadRequestException(
                    "Context result limit must be at least 1"
            );
        }

        if (limit > MAX_CONTEXT_LIMIT) {
            throw new BadRequestException(
                    "Context result limit cannot exceed " +
                            MAX_CONTEXT_LIMIT
            );
        }

        return limit;
    }

    private SemanticSearchResponse toContextResponse(
            DocumentChunkSearchResult result
    ) {
        if (result == null) {
            throw new AiModelResponseException(
                    "Document context returned an invalid result"
            );
        }

        return new SemanticSearchResponse(
                result.getChunkId(),
                result.getDocumentId(),
                result.getDocumentTitle(),
                result.getChunkIndex(),
                result.getContent(),
                null,
                null
        );
    }

    private List<SemanticSearchResponse> searchInDocumentsInternal(
            UUID workspaceId,
            List<UUID> documentIds,
            String query,
            Integer limit,
            User currentUser,
            boolean allowFallback
    ) {
        String normalizedQuery =
                validateAndNormalizeQuery(query);

        int normalizedLimit =
                validateAndNormalizeLimit(limit);

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
                databaseResults,
                allowFallback
        );
    }

    private String validateAndNormalizeQuery(
            String query
    ) {
        if (query == null || query.isBlank()) {
            throw new BadRequestException(
                    ApiErrorCodes.SEARCH_QUERY_REQUIRED,
                    "Search query cannot be empty"
            );
        }

        String normalizedQuery = query.trim();

        if (normalizedQuery.length() > MAX_QUERY_LENGTH) {
            throw new BadRequestException(
                    ApiErrorCodes.SEARCH_QUERY_TOO_LONG,
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
                    ApiErrorCodes.SEARCH_LIMIT_INVALID,
                    "Search result limit must be at least 1"
            );
        }

        if (limit > MAX_LIMIT) {
            throw new BadRequestException(
                    ApiErrorCodes.SEARCH_LIMIT_INVALID,
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
                    ApiErrorCodes.DOCUMENT_SELECTION_REQUIRED,
                    "At least one document must be provided"
            );
        }

        List<UUID> uniqueDocumentIds =
                documentIds.stream()
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList();

        if (uniqueDocumentIds.isEmpty()) {
            throw new BadRequestException(
                    ApiErrorCodes.DOCUMENT_SELECTION_REQUIRED,
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
            List<DocumentChunkSearchResult> databaseResults,
            boolean allowFallback
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
         * Log all results before filtering so we can inspect
         * rejected similarities while tuning retrieval.
         */
        logSearchResults(
                workspaceId,
                query,
                mappedResults
        );

        List<SemanticSearchResponse> strictResults =
                mappedResults.stream()
                        .filter(this::meetsSimilarityThreshold)
                        .toList();

        /*
         * If normal semantic retrieval found relevant chunks,
         * use those and do not fall back.
         */
        if (!strictResults.isEmpty()) {
            return strictResults;
        }

        /*
         * Normal workspace-wide search remains strict.
         *
         * Fallback is only allowed for explicitly selected
         * documents.
         */
        if (!allowFallback) {
            return List.of();
        }

        /*
         * If the user explicitly selected documents but none
         * passed the normal threshold, allow a softer threshold.
         *
         * This helps broad questions such as:
         *
         * "What animals are mentioned?"
         * "What topics are covered?"
         * "What information is in this document?"
         */
        List<SemanticSearchResponse> fallbackResults =
                mappedResults.stream()
                        .filter(
                                this::meetsFallbackSimilarityThreshold
                        )
                        .toList();

        log.debug(
                "Attached-document semantic fallback: " +
                        "workspace={}, strictThreshold={}, " +
                        "fallbackThreshold={}, candidates={}, accepted={}",
                workspaceId,
                minimumSimilarity,
                fallbackMinimumSimilarity,
                mappedResults.size(),
                fallbackResults.size()
        );

        return fallbackResults;
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
         * pgvector cosine distance:
         *
         * similarity = 1 - distance
         */
        double similarity =
                1.0 - distance;

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
                Double.isFinite(
                        result.similarity()
                ) &&
                result.similarity() >=
                        minimumSimilarity;
    }

    private boolean meetsFallbackSimilarityThreshold(
            SemanticSearchResponse result
    ) {
        return result.similarity() != null &&
                Double.isFinite(
                        result.similarity()
                ) &&
                result.similarity() >=
                        fallbackMinimumSimilarity;
    }

    private String toVectorLiteral(
            float[] embedding
    ) {
        StringBuilder vector =
                new StringBuilder(
                        embedding.length * 12
                );

        vector.append('[');

        for (
                int index = 0;
                index < embedding.length;
                index++
        ) {
            if (index > 0) {
                vector.append(',');
            }

            vector.append(
                    Float.toString(
                            embedding[index]
                    )
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
        log.debug(
                "Semantic search completed: " +
                        "workspace={}, queryLength={}, " +
                        "threshold={}, fallbackThreshold={}, results={}",
                workspaceId,
                query.length(),
                minimumSimilarity,
                fallbackMinimumSimilarity,
                results.size()
        );

        for (SemanticSearchResponse result : results) {

            boolean strictAccepted =
                    meetsSimilarityThreshold(
                            result
                    );

            boolean fallbackAccepted =
                    meetsFallbackSimilarityThreshold(
                            result
                    );

            log.debug(
                    "Semantic result: " +
                            "document={}, chunk={}, similarity={}, " +
                            "distance={}, strictAccepted={}, " +
                            "fallbackAccepted={}",
                    result.documentTitle(),
                    result.chunkIndex(),
                    result.similarity(),
                    result.distance(),
                    strictAccepted,
                    fallbackAccepted
            );
        }
    }
}