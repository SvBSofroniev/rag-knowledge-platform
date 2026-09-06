package src.chat.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import src.chat.dto.ChatAnswerResponse;
import src.chat.dto.ChatMessageResponse;
import src.chat.dto.SendChatMessageRequest;
import src.chat.repository.AiQueryRepository;
import src.chat.repository.ChatMessageRepository;
import src.chat.repository.ChatSessionRepository;
import src.common.exception.ApiErrorCodes;
import src.common.exception.BadRequestException;
import src.common.language.ResponseLanguage;
import src.common.service.ResponseLanguageService;
import src.entity.AiQuery;
import src.entity.ChatMessage;
import src.entity.ChatSession;
import src.entity.User;
import src.rag.dto.ConversationMessage;
import src.rag.dto.RagAnswerResponse;
import src.rag.dto.RagExplanationResponse;
import src.rag.dto.SemanticSearchResponse;
import src.rag.model.RetrievalStrategy;
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

    private static final int MAX_MESSAGE_LENGTH =
            5_000;

    private final ChatSessionService
            chatSessionService;

    private final ChatMessageRepository
            chatMessageRepository;

    private final ChatSessionRepository
            chatSessionRepository;

    private final AiQueryRepository
            aiQueryRepository;

    private final RagService
            ragService;

    private final ChatDocumentContextService
            chatDocumentContextService;

    private final ChatMessageSourceService
            chatMessageSourceService;

    private final ResponseLanguageService
            responseLanguageService;

    @Value("${spring.ai.ollama.chat.model:gemma3:4b}")
    private String chatModelName;

    @Transactional
    public ChatAnswerResponse sendMessage(
            UUID sessionId,
            SendChatMessageRequest request,
            User currentUser
    ) {
        validateRequest(
                request
        );

        ChatSession session =
                chatSessionService
                        .getAccessibleSession(
                                sessionId,
                                currentUser
                        );

        String question =
                request.message()
                        .trim();

        /*
         * Load previous messages before saving the current
         * question so that it is not duplicated in the
         * conversation history sent to RAG.
         */
        List<ConversationMessage> conversationHistory =
                loadRecentConversation(
                        session
                );

        ChatMessage userMessage =
                saveMessage(
                        session,
                        SenderType.USER,
                        question
                );

        List<UUID> attachedDocumentIds =
                chatDocumentContextService
                        .getAttachedDocumentIds(
                                sessionId,
                                currentUser
                        );

        /*
         * RAG requires at least one attached document.
         *
         * This branch does not invoke RagService or the LLM,
         * therefore the informational response must be
         * localized here.
         */
        if (attachedDocumentIds.isEmpty()) {

            ResponseLanguage responseLanguage =
                    responseLanguageService.detect(
                            question
                    );

            String noDocumentsMessage =
                    getNoDocumentsAttachedMessage(
                            responseLanguage
                    );

            ChatMessage assistantMessage =
                    saveMessage(
                            session,
                            SenderType.ASSISTANT,
                            noDocumentsMessage
                    );

            updateSessionTimestamp(
                    session
            );

            return new ChatAnswerResponse(
                    session.getId(),

                    toMessageResponse(
                            userMessage,
                            List.of()
                    ),

                    toMessageResponse(
                            assistantMessage,
                            List.of()
                    ),

                    List.of()
            );
        }

        long startedAt =
                System.nanoTime();

        RagAnswerResponse ragAnswer =
                ragService.answer(
                        session.getWorkspace()
                                .getId(),
                        question,
                        attachedDocumentIds,
                        conversationHistory,
                        currentUser
                );

        long responseTimeMs =
                (System.nanoTime() -
                        startedAt)
                        / 1_000_000;

        /*
         * Persist the retrieval strategy with the assistant
         * message so Explainable RAG can be reconstructed
         * when the conversation is loaded again.
         */
        RetrievalStrategy retrievalStrategy =
                ragAnswer.explanation() == null
                        ? null
                        : ragAnswer.explanation()
                        .retrievalStrategy();

        ChatMessage assistantMessage =
                saveMessage(
                        session,
                        SenderType.ASSISTANT,
                        ragAnswer.answer(),
                        retrievalStrategy
                );

        /*
         * Persist the exact chunks used by RAG.
         */
        chatMessageSourceService.saveSources(
                assistantMessage,
                ragAnswer.sources()
        );

        updateSessionTimestamp(
                session
        );

        saveAiQuery(
                session,
                currentUser,
                question,
                ragAnswer.answer(),
                responseTimeMs
        );

        return new ChatAnswerResponse(
                session.getId(),

                toMessageResponse(
                        userMessage,
                        List.of()
                ),

                toMessageResponse(
                        assistantMessage,
                        ragAnswer.sources(),
                        ragAnswer.explanation()
                ),

                ragAnswer.sources()
        );
    }

    /*
     * ---------------------------------------------------------
     * VALIDATION
     * ---------------------------------------------------------
     */
    private void validateRequest(
            SendChatMessageRequest request
    ) {
        if (request == null) {
            throw new BadRequestException(
                    ApiErrorCodes.CHAT_MESSAGE_REQUEST_REQUIRED,
                    "Message request cannot be empty"
            );
        }

        String message =
                request.message();

        if (message == null ||
                message.isBlank()) {

            throw new BadRequestException(
                    ApiErrorCodes.CHAT_MESSAGE_REQUIRED,
                    "Message cannot be empty"
            );
        }

        if (message.length() >
                MAX_MESSAGE_LENGTH) {

            throw new BadRequestException(
                    ApiErrorCodes.CHAT_MESSAGE_TOO_LONG,
                    "Message cannot exceed " +
                            MAX_MESSAGE_LENGTH +
                            " characters"
            );
        }
    }

    /*
     * ---------------------------------------------------------
     * MESSAGE PERSISTENCE
     * ---------------------------------------------------------
     */
    private ChatMessage saveMessage(
            ChatSession session,
            SenderType senderType,
            String content
    ) {
        return saveMessage(
                session,
                senderType,
                content,
                null
        );
    }

    private ChatMessage saveMessage(
            ChatSession session,
            SenderType senderType,
            String content,
            RetrievalStrategy retrievalStrategy
    ) {
        ChatMessage message =
                new ChatMessage();

        message.setSession(
                session
        );

        message.setSenderType(
                senderType
        );

        message.setContent(
                content
        );

        message.setRetrievalStrategy(
                retrievalStrategy
        );

        return chatMessageRepository.save(
                message
        );
    }

    /*
     * ---------------------------------------------------------
     * SESSION
     * ---------------------------------------------------------
     */
    private void updateSessionTimestamp(
            ChatSession session
    ) {
        session.setUpdatedAt(
                LocalDateTime.now()
        );

        chatSessionRepository.save(
                session
        );
    }

    /*
     * ---------------------------------------------------------
     * AI QUERY LOGGING
     * ---------------------------------------------------------
     */
    private void saveAiQuery(
            ChatSession session,
            User currentUser,
            String question,
            String answer,
            long responseTimeMs
    ) {
        AiQuery aiQuery =
                new AiQuery();

        aiQuery.setUser(
                currentUser
        );

        aiQuery.setChatSession(
                session
        );

        aiQuery.setQueryText(
                question
        );

        aiQuery.setResponseText(
                answer
        );

        aiQuery.setModelName(
                chatModelName
        );

        aiQuery.setResponseTimeMs(
                Math.toIntExact(
                        Math.min(
                                responseTimeMs,
                                Integer.MAX_VALUE
                        )
                )
        );

        /*
         * Token counts remain null because the current
         * Ollama integration does not expose them through
         * RagAnswerResponse.
         */
        aiQuery.setPromptTokens(null);
        aiQuery.setCompletionTokens(null);
        aiQuery.setTotalTokens(null);

        aiQueryRepository.save(
                aiQuery
        );
    }

    /*
     * ---------------------------------------------------------
     * RESPONSE MAPPING
     * ---------------------------------------------------------
     */
    private ChatMessageResponse toMessageResponse(
            ChatMessage message,
            List<SemanticSearchResponse> sources
    ) {
        return toMessageResponse(
                message,
                sources,
                null
        );
    }

    private ChatMessageResponse toMessageResponse(
            ChatMessage message,
            List<SemanticSearchResponse> sources,
            RagExplanationResponse explanation
    ) {
        return new ChatMessageResponse(
                message.getId(),
                message.getSenderType(),
                message.getContent(),
                message.getCreatedAt(),
                sources == null
                        ? List.of()
                        : sources,
                explanation
        );
    }

    /*
     * ---------------------------------------------------------
     * CONVERSATION HISTORY
     * ---------------------------------------------------------
     */
    private List<ConversationMessage> loadRecentConversation(
            ChatSession session
    ) {
        List<ChatMessage> messages =
                new ArrayList<>(
                        chatMessageRepository
                                .findTop10BySessionOrderByCreatedAtDesc(
                                        session
                                )
                );

        /*
         * Repository returns newest-first.
         * RAG receives oldest-to-newest.
         */
        Collections.reverse(
                messages
        );

        return messages.stream()
                .filter(message ->
                        message.getSenderType() ==
                                SenderType.USER ||
                                message.getSenderType() ==
                                        SenderType.ASSISTANT
                )
                .filter(message ->
                        message.getContent() != null &&
                                !message.getContent()
                                        .isBlank()
                )
                .map(message ->
                        new ConversationMessage(
                                message.getSenderType(),
                                message.getContent()
                        )
                )
                .toList();
    }

    /*
     * ---------------------------------------------------------
     * LOCALIZED SYSTEM RESPONSES
     * ---------------------------------------------------------
     */
    private String getNoDocumentsAttachedMessage(
            ResponseLanguage responseLanguage
    ) {
        return switch (responseLanguage) {

            case BULGARIAN ->
                    "Към този чат няма прикачени документи. " +
                            "Прикачете поне един документ, преди да зададете " +
                            "въпрос, свързан с документите.";

            case ENGLISH ->
                    "No documents are attached to this chat. " +
                            "Attach at least one document before asking " +
                            "document-related questions.";
        };
    }
}