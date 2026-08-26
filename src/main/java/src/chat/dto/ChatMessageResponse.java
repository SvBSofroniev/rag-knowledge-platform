package src.chat.dto;

import src.rag.dto.SemanticSearchResponse;
import src.util.SenderType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ChatMessageResponse(
        UUID id,
        SenderType senderType,
        String content,
        LocalDateTime createdAt,
        List<SemanticSearchResponse> sources
) {
}