package src.document.dto;

import src.document.util.DocumentStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record DocumentResponse(
        UUID id,
        String originalFilename,
        String mimeType,
        Long size,
        DocumentStatus status,
        UUID uploadedById,
        String uploadedByUsername,
        LocalDateTime createdAt
) {}