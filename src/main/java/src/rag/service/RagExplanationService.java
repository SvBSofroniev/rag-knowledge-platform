package src.rag.service;

import org.springframework.stereotype.Service;
import src.rag.dto.EvidenceStrength;
import src.rag.dto.RagExplanationResponse;
import src.rag.dto.SemanticSearchResponse;
import src.rag.model.RetrievalStrategy;

import java.util.List;
import java.util.Objects;

@Service
public class RagExplanationService {

    private static final double STRONG_BEST_SIMILARITY = 0.75;
    private static final double STRONG_AVERAGE_SIMILARITY = 0.50;

    private static final double MODERATE_BEST_SIMILARITY = 0.50;

    public RagExplanationResponse build(
            RetrievalStrategy retrievalStrategy,
            List<SemanticSearchResponse> sources
    ) {
        List<SemanticSearchResponse> normalizedSources =
                sources == null
                        ? List.of()
                        : sources.stream()
                        .filter(Objects::nonNull)
                        .toList();

        int supportingChunks =
                normalizedSources.size();

        int supportingDocuments =
                (int) normalizedSources.stream()
                        .map(
                                SemanticSearchResponse::documentId
                        )
                        .filter(Objects::nonNull)
                        .distinct()
                        .count();

        List<Double> similarities =
                normalizedSources.stream()
                        .map(
                                SemanticSearchResponse::similarity
                        )
                        .filter(Objects::nonNull)
                        .filter(Double::isFinite)
                        .toList();

        /*
         * Full document context does not use semantic ranking.
         *
         * Therefore similarity-based evidence strength is not
         * applicable and must not be invented.
         */
        if (similarities.isEmpty()) {
            return new RagExplanationResponse(
                    retrievalStrategy,
                    EvidenceStrength.NOT_APPLICABLE,
                    supportingChunks,
                    supportingDocuments,
                    null,
                    null
            );
        }

        double bestSimilarity =
                similarities.stream()
                        .mapToDouble(
                                Double::doubleValue
                        )
                        .max()
                        .orElse(0.0);

        double averageSimilarity =
                similarities.stream()
                        .mapToDouble(
                                Double::doubleValue
                        )
                        .average()
                        .orElse(0.0);

        EvidenceStrength evidenceStrength =
                determineEvidenceStrength(
                        bestSimilarity,
                        averageSimilarity
                );

        return new RagExplanationResponse(
                retrievalStrategy,
                evidenceStrength,
                supportingChunks,
                supportingDocuments,
                bestSimilarity,
                averageSimilarity
        );
    }

    private EvidenceStrength determineEvidenceStrength(
            double bestSimilarity,
            double averageSimilarity
    ) {
        if (bestSimilarity >= STRONG_BEST_SIMILARITY &&
                averageSimilarity >=
                        STRONG_AVERAGE_SIMILARITY) {

            return EvidenceStrength.STRONG;
        }

        if (bestSimilarity >=
                MODERATE_BEST_SIMILARITY) {

            return EvidenceStrength.MODERATE;
        }

        return EvidenceStrength.WEAK;
    }
}