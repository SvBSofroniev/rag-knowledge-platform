package src.workspace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SendWorkspaceMessageRequest(

        @NotBlank
        @Size(max = 2_000)
        String content

) {
}