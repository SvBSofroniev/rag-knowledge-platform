package src.workspace.dto;

import src.workspace.util.WorkspaceRole;

public record UpdateWorkspaceRoleRequest(
        WorkspaceRole role
) {}
