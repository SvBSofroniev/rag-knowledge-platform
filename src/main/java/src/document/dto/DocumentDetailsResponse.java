package src.document.dto;

import src.document.util.DocumentStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record DocumentDetailsResponse(
        UUID id,
        UUID workspaceId,
        String workspaceName,
        String title,
        String originalFilename,
        String fileType,
        Long fileSize,
        DocumentStatus status,
        String processingError,
        UUID uploadedById,
        String uploadedByUsername,
        long chunkCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}