package src.rag.dto;

import src.rag.model.RetrievalStrategy;

public record RagExplanationResponse(

        RetrievalStrategy retrievalStrategy,

        EvidenceStrength evidenceStrength,

        int supportingChunks,

        int supportingDocuments,

        Double bestSimilarity,

        Double averageSimilarity

) {
}