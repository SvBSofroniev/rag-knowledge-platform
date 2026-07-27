package src.rag.dto;

import java.util.List;

public record RagAnswerResponse(
        String question,
        String answer,
        List<SemanticSearchResponse> sources
) {
}