package src.chat.dto;

import jakarta.validation.constraints.Size;

public record CreateChatSessionRequest(

        @Size(
                max = 255,
                message = "Chat title cannot exceed 255 characters"
        )
        String title
) {
}