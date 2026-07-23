package src.document.service;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class TextChunker {

    private static final int DEFAULT_CHUNK_SIZE = 1_500;
    private static final int DEFAULT_OVERLAP = 200;

    public List<String> chunk(String text) {
        return chunk(
                text,
                DEFAULT_CHUNK_SIZE,
                DEFAULT_OVERLAP
        );
    }

    public List<String> chunk(
            String text,
            int chunkSize,
            int overlap
    ) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        if (chunkSize <= 0) {
            throw new IllegalArgumentException(
                    "Chunk size must be greater than zero"
            );
        }

        if (overlap < 0 || overlap >= chunkSize) {
            throw new IllegalArgumentException(
                    "Overlap must be smaller than chunk size"
            );
        }

        String normalizedText = normalize(text);
        List<String> chunks = new ArrayList<>();

        int start = 0;

        while (start < normalizedText.length()) {
            int expectedEnd = Math.min(
                    start + chunkSize,
                    normalizedText.length()
            );

            int end = findNaturalBoundary(
                    normalizedText,
                    start,
                    expectedEnd
            );

            String content = normalizedText
                    .substring(start, end)
                    .trim();

            if (!content.isBlank()) {
                chunks.add(content);
            }

            if (end >= normalizedText.length()) {
                break;
            }

            start = Math.max(
                    end - overlap,
                    start + 1
            );
        }

        return chunks;
    }

    private int findNaturalBoundary(
            String text,
            int start,
            int expectedEnd
    ) {
        if (expectedEnd >= text.length()) {
            return text.length();
        }

        int minimumBoundary =
                start + (int) ((expectedEnd - start) * 0.7);

        int paragraphBoundary = text.lastIndexOf(
                "\n\n",
                expectedEnd
        );

        if (paragraphBoundary >= minimumBoundary) {
            return paragraphBoundary;
        }

        int sentenceBoundary = text.lastIndexOf(
                ". ",
                expectedEnd
        );

        if (sentenceBoundary >= minimumBoundary) {
            return sentenceBoundary + 1;
        }

        int whitespaceBoundary = text.lastIndexOf(
                ' ',
                expectedEnd
        );

        if (whitespaceBoundary >= minimumBoundary) {
            return whitespaceBoundary;
        }

        return expectedEnd;
    }

    private String normalize(String text) {
        return text
                .replace("\r\n", "\n")
                .replace('\r', '\n')

                // Preserve actual paragraph breaks temporarily.
                .replaceAll("\\n\\s*\\n+", "<PARAGRAPH_BREAK>")

                // Join PDF visual line wrapping.
                .replaceAll("\\s*\\n\\s*", " ")

                // Restore paragraph breaks.
                .replace("<PARAGRAPH_BREAK>", "\n\n")

                // Normalize repeated spaces.
                .replaceAll("[\\t ]+", " ")

                .trim();
    }
}
