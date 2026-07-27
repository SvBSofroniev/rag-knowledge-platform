package src.chat.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ChatSessionResponse(
        UUID id,
        UUID workspaceId,
        String workspaceName,
        String title,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
