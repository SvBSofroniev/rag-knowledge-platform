package src.workspace.dto;

import src.workspace.util.WorkspaceRole;

import java.time.LocalDateTime;
import java.util.UUID;

public record WorkspaceMemberResponse(
        String memberId,
        UUID userId,
        String username,
        String email,
        WorkspaceRole role,
        LocalDateTime joinedAt
) {}