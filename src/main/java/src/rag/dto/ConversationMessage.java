package src.rag.dto;

import src.util.SenderType;

public record ConversationMessage(
        SenderType senderType,
        String content
) {
}