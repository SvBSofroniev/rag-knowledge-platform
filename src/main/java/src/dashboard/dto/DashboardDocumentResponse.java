package src.dashboard.dto;

import src.document.util.DocumentStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record DashboardDocumentResponse(
        UUID id,
        UUID workspaceId,
        String workspaceName,
        String title,
        String originalFilename,
        DocumentStatus status,
        LocalDateTime createdAt
) {
}
