package src.workspace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateWorkspaceRequest(

        @NotBlank(message = "Workspace name cannot be empty")
        @Size(
                max = 100,
                message = "Workspace name cannot exceed 100 characters"
        )
        String name,

        String description
) {
}
