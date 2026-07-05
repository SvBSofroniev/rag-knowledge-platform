package src.workspace.dto;

public record CreateWorkspaceRequest(
        String name,
        String description
) {}