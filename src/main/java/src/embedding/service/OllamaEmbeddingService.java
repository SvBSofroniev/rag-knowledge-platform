package src.embedding.service;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OllamaEmbeddingService implements EmbeddingService {

    private static final int EXPECTED_DIMENSIONS = 768;

    private final EmbeddingModel embeddingModel;

    @Override
    public float[] generateEmbedding(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException(
                    "Text for embedding cannot be empty"
            );
        }

        float[] embedding = embeddingModel.embed(text);

        if (embedding == null) {
            throw new RuntimeException(
                    "Embedding model returned null"
            );
        }

        if (embedding.length != EXPECTED_DIMENSIONS) {
            throw new RuntimeException(
                    "Unexpected embedding dimensions. Expected " +
                            EXPECTED_DIMENSIONS +
                            " but received " +
                            embedding.length
            );
        }

        return embedding;
    }
}