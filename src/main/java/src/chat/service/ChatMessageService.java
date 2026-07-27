package src.chat.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import src.chat.dto.ChatAnswerResponse;
import src.chat.dto.ChatMessageResponse;
import src.chat.dto.SendChatMessageRequest;
import src.chat.repository.AiQueryRepository;
import src.chat.repository.ChatMessageRepository;
import src.chat.repository.ChatSessionRepository;
import src.entity.AiQuery;
import src.entity.ChatMessage;
import src.entity.ChatSession;
import src.entity.User;
import src.rag.dto.ConversationMessage;
import src.rag.dto.RagAnswerResponse;
import src.rag.service.RagService;
import src.util.SenderType;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private final ChatSessionService chatSessionService;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatSessionRepository chatSessionRepository;
    private final AiQueryRepository aiQueryRepository;
    private final RagService ragService;
    private final ChatDocumentContextService chatDocumentContextService;

    @Value("${spring.ai.ollama.chat.model:gemma3:4b}")
    private String chatModelName;

    public ChatAnswerResponse sendMessage(
            UUID sessionId,
            SendChatMessageRequest request,
            User currentUser
    ) {
        ChatSession session =
                chatSessionService.getAccessibleSession(
                        sessionId,
                        currentUser
                );

        String question = request.message().trim();

        /*
         * Load previous messages before saving the current question,
         * otherwise the question would appear twice in the prompt.
         */
        List<ConversationMessage> conversationHistory =
                loadRecentConversation(session);

        ChatMessage userMessage = saveMessage(
                session,
                SenderType.USER,
                question
        );

        List<UUID> attachedDocumentIds =
                chatDocumentContextService.getAttachedDocumentIds(
                        session
                );

        long startedAt = System.nanoTime();

        RagAnswerResponse ragAnswer = ragService.answer(
                session.getWorkspace().getId(),
                question,
                attachedDocumentIds,
                conversationHistory,
                currentUser
        );

        long responseTimeMs =
                (System.nanoTime() - startedAt) / 1_000_000;

        ChatMessage assistantMessage = saveMessage(
                session,
                SenderType.ASSISTANT,
                ragAnswer.answer()
        );

        updateSessionTimestamp(session);

        saveAiQuery(
                session,
                currentUser,
                question,
                ragAnswer.answer(),
                responseTimeMs
        );

        return new ChatAnswerResponse(
                session.getId(),
                toMessageResponse(userMessage),
                toMessageResponse(assistantMessage),
                ragAnswer.sources()
        );
    }

    private ChatMessage saveMessage(
            ChatSession session,
            SenderType senderType,
            String content
    ) {
        ChatMessage message = new ChatMessage();
        message.setSession(session);
        message.setSenderType(senderType);
        message.setContent(content);

        return chatMessageRepository.save(message);
    }

    private void updateSessionTimestamp(
            ChatSession session
    ) {
        session.setUpdatedAt(LocalDateTime.now());
        chatSessionRepository.save(session);
    }

    private void saveAiQuery(
            ChatSession session,
            User currentUser,
            String question,
            String answer,
            long responseTimeMs
    ) {
        AiQuery aiQuery = new AiQuery();
        aiQuery.setUser(currentUser);
        aiQuery.setChatSession(session);
        aiQuery.setQueryText(question);
        aiQuery.setResponseText(answer);
        aiQuery.setModelName(chatModelName);
        aiQuery.setResponseTimeMs(
                Math.toIntExact(
                        Math.min(
                                responseTimeMs,
                                Integer.MAX_VALUE
                        )
                )
        );

        /*
         * Token counts remain null for now because the current
         * local Ollama integration does not yet expose them
         * through RagAnswerResponse.
         */
        aiQuery.setPromptTokens(null);
        aiQuery.setCompletionTokens(null);
        aiQuery.setTotalTokens(null);

        aiQueryRepository.save(aiQuery);
    }

    private ChatMessageResponse toMessageResponse(
            ChatMessage message
    ) {
        return new ChatMessageResponse(
                message.getId(),
                message.getSenderType(),
                message.getContent(),
                message.getCreatedAt()
        );
    }

    private List<ConversationMessage> loadRecentConversation(
            ChatSession session
    ) {
        List<ChatMessage> messages = new ArrayList<>(
                chatMessageRepository
                        .findTop10BySessionOrderByCreatedAtDesc(session)
        );

        /*
         * The database query returns newest-first.
         * The model should receive oldest-to-newest.
         */
        Collections.reverse(messages);

        return messages.stream()
                .filter(message ->
                        message.getSenderType() == SenderType.USER ||
                                message.getSenderType() == SenderType.ASSISTANT
                )
                .filter(message ->
                        message.getContent() != null &&
                                !message.getContent().isBlank()
                )
                .map(message -> new ConversationMessage(
                        message.getSenderType(),
                        message.getContent()
                ))
                .toList();
    }
}