package src.chat.dto;

import src.util.SenderType;

import java.time.LocalDateTime;
import java.util.UUID;

public record ChatMessageResponse(
        UUID id,
        SenderType senderType,
        String content,
        LocalDateTime createdAt
) {
}