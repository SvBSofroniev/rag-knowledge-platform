package src.embedding.service;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;
import src.common.exception.AiModelResponseException;
import src.common.exception.AiServiceUnavailableException;
import src.common.exception.BadRequestException;

@Service
@RequiredArgsConstructor
public class OllamaEmbeddingService implements EmbeddingService {

    private static final int EXPECTED_DIMENSIONS = 768;
    private static final int MAX_TEXT_LENGTH = 20_000;

    private final EmbeddingModel embeddingModel;

    @Override
    public float[] generateEmbedding(String text) {
        String normalizedText = validateAndNormalizeText(text);

        float[] embedding;

        try {
            embedding = embeddingModel.embed(normalizedText);

        } catch (Exception exception) {
            throw new AiServiceUnavailableException(
                    "The local Ollama embedding service is unavailable",
                    exception
            );
        }

        validateEmbedding(embedding);

        return embedding;
    }

    private String validateAndNormalizeText(String text) {
        if (text == null || text.isBlank()) {
            throw new BadRequestException(
                    "Text for embedding cannot be empty"
            );
        }

        String normalizedText = text.trim();

        if (normalizedText.length() > MAX_TEXT_LENGTH) {
            throw new BadRequestException(
                    "Text for embedding cannot exceed " +
                            MAX_TEXT_LENGTH +
                            " characters"
            );
        }

        return normalizedText;
    }

    private void validateEmbedding(float[] embedding) {
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
}