package src.chat.dto;

import src.document.util.DocumentStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record AttachedDocumentResponse(
        UUID contextId,
        UUID documentId,
        String title,
        String originalFilename,
        DocumentStatus status,
        LocalDateTime attachedAt
) {
}