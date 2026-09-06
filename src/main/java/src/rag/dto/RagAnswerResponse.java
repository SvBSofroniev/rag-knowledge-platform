package src.rag.dto;

import java.util.List;

public record RagAnswerResponse(

        String question,

        String answer,

        List<SemanticSearchResponse> sources,

        RagExplanationResponse explanation

) {

    /*
     * Keeps older call sites compatible.
     */
    public RagAnswerResponse(
            String question,
            String answer,
            List<SemanticSearchResponse> sources
    ) {
        this(
                question,
                answer,
                sources,
                null
        );
    }
}