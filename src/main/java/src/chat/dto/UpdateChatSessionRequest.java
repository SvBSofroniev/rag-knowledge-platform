package src.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateChatSessionRequest(

        @NotBlank(
                message = "Chat session title cannot be empty"
        )
        @Size(
                max = 255,
                message = "Chat session title cannot exceed 255 characters"
        )
        String title
) {
}