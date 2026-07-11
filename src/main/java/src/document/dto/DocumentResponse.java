package src.document.dto;

import src.document.util.DocumentStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record DocumentResponse(
        UUID id,
        String title,
        String originalFilename,
        String fileType,
        Long fileSize,
        DocumentStatus status,
        String processingError,
        UUID uploadedById,
        String uploadedByUsername,
        LocalDateTime createdAt
) {}