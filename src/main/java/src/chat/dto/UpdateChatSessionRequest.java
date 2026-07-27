package src.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateChatSessionRequest(

        @NotBlank(message = "Chat title cannot be empty")
        @Size(
                max = 255,
                message = "Chat title cannot exceed 255 characters"
        )
        String title
) {
}