package src.rag.dto;

import java.util.UUID;

public record SemanticSearchResponse(
        UUID chunkId,
        UUID documentId,
        String documentTitle,
        Integer chunkIndex,
        String content,
        Double distance,
        Double similarity
) {
}