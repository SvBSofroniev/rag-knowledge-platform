package src.workspace.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record WorkspaceMessageResponse(

        UUID id,

        UUID userId,

        String username,

        String displayName,

        String content,

        LocalDateTime createdAt,

        boolean ownMessage

) {
}