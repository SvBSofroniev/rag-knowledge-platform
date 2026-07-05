package src.workspace.dto;

import src.workspace.util.WorkspaceRole;

public record WorkspaceResponse(
        String id,
        String name,
        String description,
        WorkspaceRole myRole
) {}