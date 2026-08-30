package src.document.dto;


import src.document.util.DocumentStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record GlobalDocumentResponse(
        UUID id,
        UUID workspaceId,
        String workspaceName,
        String title,
        String originalFilename,
        String fileType,
        Long fileSize,
        DocumentStatus status,
        UUID uploadedById,
        String uploadedByUsername,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}