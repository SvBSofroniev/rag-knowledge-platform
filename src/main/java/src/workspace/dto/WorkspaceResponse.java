package src.workspace.dto;

import src.workspace.util.WorkspaceRole;

import java.util.UUID;

public record WorkspaceResponse(
        UUID id,
        String name,
        String description,
        WorkspaceRole myRole
) {}