package src.document.dto;

import java.util.UUID;

public record DocumentContentResponse(
        UUID documentId,
        String title,
        long chunkCount,
        String content
) {
}