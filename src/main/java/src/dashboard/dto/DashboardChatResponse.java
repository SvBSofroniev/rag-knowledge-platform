package src.dashboard.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record DashboardChatResponse(
        UUID id,
        UUID workspaceId,
        String workspaceName,
        String title,
        LocalDateTime updatedAt
) {
}