package src.chat.dto;

import src.rag.dto.SemanticSearchResponse;

import java.util.List;
import java.util.UUID;

public record ChatAnswerResponse(
        UUID sessionId,
        ChatMessageResponse userMessage,
        ChatMessageResponse assistantMessage,
        List<SemanticSearchResponse> sources
) {
}