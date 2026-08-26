package src.dashboard.dto;

import src.workspace.util.WorkspaceRole;

import java.time.LocalDateTime;
import java.util.UUID;

public record DashboardWorkspaceResponse(
        UUID id,
        String name,
        String description,
        WorkspaceRole myRole,
        long documentCount,
        LocalDateTime updatedAt
) {
}