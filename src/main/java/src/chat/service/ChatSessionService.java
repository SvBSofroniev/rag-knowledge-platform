package src.chat.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import src.chat.dto.ChatMessageResponse;
import src.chat.dto.ChatSessionResponse;
import src.chat.dto.CreateChatSessionRequest;
import src.chat.dto.UpdateChatSessionRequest;
import src.chat.repository.ChatMessageRepository;
import src.chat.repository.ChatSessionRepository;
import src.common.exception.ApiErrorCodes;
import src.common.exception.BadRequestException;
import src.common.exception.ResourceNotFoundException;
import src.entity.ChatMessage;
import src.entity.ChatSession;
import src.entity.User;
import src.entity.Workspace;
import src.entity.WorkspaceMember;
import src.rag.dto.RagExplanationResponse;
import src.rag.dto.SemanticSearchResponse;
import src.rag.service.RagExplanationService;
import src.util.SenderType;
import src.workspace.service.WorkspacePermissionService;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatSessionService {

    private static final String DEFAULT_TITLE =
            "New chat";

    private static final int MAX_TITLE_LENGTH =
            255;

    private final ChatSessionRepository
            chatSessionRepository;

    private final ChatMessageRepository
            chatMessageRepository;

    private final WorkspacePermissionService
            workspacePermissionService;

    private final ChatMessageSourceService
            chatMessageSourceService;

    private final RagExplanationService
            ragExplanationService;

    /*
     * ---------------------------------------------------------
     * CREATE SESSION
     * ---------------------------------------------------------
     */
    @Transactional
    public ChatSessionResponse createSession(
            UUID workspaceId,
            CreateChatSessionRequest request,
            User currentUser
    ) {
        WorkspaceMember membership =
                workspacePermissionService.requireMember(
                        workspaceId,
                        currentUser
                );

        Workspace workspace =
                membership.getWorkspace();

        ChatSession session =
                new ChatSession();

        session.setWorkspace(
                workspace
        );

        session.setUser(
                currentUser
        );

        session.setTitle(
                resolveTitle(request)
        );

        ChatSession savedSession =
                chatSessionRepository.save(
                        session
                );

        return toSessionResponse(
                savedSession
        );
    }

    /*
     * ---------------------------------------------------------
     * LIST WORKSPACE SESSIONS
     * ---------------------------------------------------------
     */
    @Transactional(readOnly = true)
    public List<ChatSessionResponse> getMySessions(
            UUID workspaceId,
            User currentUser
    ) {
        WorkspaceMember membership =
                workspacePermissionService.requireMember(
                        workspaceId,
                        currentUser
                );

        Workspace workspace =
                membership.getWorkspace();

        return chatSessionRepository
                .findByWorkspaceAndUserOrderByUpdatedAtDesc(
                        workspace,
                        currentUser
                )
                .stream()
                .map(
                        this::toSessionResponse
                )
                .toList();
    }

    /*
     * ---------------------------------------------------------
     * LIST ALL USER SESSIONS
     * ---------------------------------------------------------
     */
    @Transactional(readOnly = true)
    public List<ChatSessionResponse> getAllMySessions(
            User currentUser
    ) {
        return chatSessionRepository
                .findAllAccessibleByUser(
                        currentUser
                )
                .stream()
                .map(
                        this::toSessionResponse
                )
                .toList();
    }

    /*
     * ---------------------------------------------------------
     * GET SESSION
     * ---------------------------------------------------------
     */
    @Transactional(readOnly = true)
    public ChatSessionResponse getSession(
            UUID sessionId,
            User currentUser
    ) {
        ChatSession session =
                getAccessibleSession(
                        sessionId,
                        currentUser
                );

        return toSessionResponse(
                session
        );
    }

    /*
     * ---------------------------------------------------------
     * GET SESSION MESSAGES
     * ---------------------------------------------------------
     *
     * Sources are persisted separately from ChatMessage.
     *
     * For assistant messages generated after Explainable RAG
     * was introduced, the retrieval strategy is also persisted
     * in ChatMessage.
     *
     * This allows the explanation to be reconstructed after:
     *
     * - page refresh
     * - reopening a chat
     * - loading historical messages
     */
    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getSessionMessages(
            UUID sessionId,
            User currentUser
    ) {
        ChatSession session =
                getAccessibleSession(
                        sessionId,
                        currentUser
                );

        return chatMessageRepository
                .findBySessionOrderByCreatedAtAsc(
                        session
                )
                .stream()
                .map(
                        this::toMessageResponse
                )
                .toList();
    }

    /*
     * ---------------------------------------------------------
     * ACCESS CONTROL
     * ---------------------------------------------------------
     */
    @Transactional(readOnly = true)
    public ChatSession getAccessibleSession(
            UUID sessionId,
            User currentUser
    ) {
        ChatSession session =
                chatSessionRepository
                        .findById(
                                sessionId
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        ApiErrorCodes.CHAT_SESSION_NOT_FOUND,
                                        "Chat session not found"
                                )
                        );

        /*
         * Verify that the current user is still a member
         * of the workspace.
         */
        workspacePermissionService.requireMember(
                session.getWorkspace()
                        .getId(),
                currentUser
        );

        /*
         * AI chat sessions are personal.
         *
         * Return 404 instead of 403 so the API does not reveal
         * the existence of another user's private chat session.
         */
        if (!session.getUser()
                .getId()
                .equals(
                        currentUser.getId()
                )) {

            throw new ResourceNotFoundException(
                    ApiErrorCodes.CHAT_SESSION_NOT_FOUND,
                    "Chat session not found"
            );
        }

        return session;
    }

    /*
     * ---------------------------------------------------------
     * UPDATE SESSION
     * ---------------------------------------------------------
     */
    @Transactional
    public ChatSessionResponse updateSession(
            UUID sessionId,
            UpdateChatSessionRequest request,
            User currentUser
    ) {
        String normalizedTitle =
                validateAndNormalizeUpdateTitle(
                        request
                );

        ChatSession session =
                getAccessibleSession(
                        sessionId,
                        currentUser
                );

        session.setTitle(
                normalizedTitle
        );

        ChatSession updatedSession =
                chatSessionRepository.save(
                        session
                );

        return toSessionResponse(
                updatedSession
        );
    }

    /*
     * ---------------------------------------------------------
     * DELETE SESSION
     * ---------------------------------------------------------
     */
    @Transactional
    public void deleteSession(
            UUID sessionId,
            User currentUser
    ) {
        ChatSession session =
                getAccessibleSession(
                        sessionId,
                        currentUser
                );

        chatSessionRepository.delete(
                session
        );
    }

    /*
     * ---------------------------------------------------------
     * TITLE HANDLING
     * ---------------------------------------------------------
     */
    private String resolveTitle(
            CreateChatSessionRequest request
    ) {
        if (request == null ||
                request.title() == null ||
                request.title().isBlank()) {

            return DEFAULT_TITLE;
        }

        String normalizedTitle =
                request.title().trim();

        validateTitleLength(
                normalizedTitle
        );

        return normalizedTitle;
    }

    private String validateAndNormalizeUpdateTitle(
            UpdateChatSessionRequest request
    ) {
        if (request == null) {
            throw new BadRequestException(
                    ApiErrorCodes.CHAT_SESSION_UPDATE_REQUIRED,
                    "Chat session update request cannot be empty"
            );
        }

        String title =
                request.title();

        if (title == null ||
                title.isBlank()) {

            throw new BadRequestException(
                    ApiErrorCodes.CHAT_TITLE_REQUIRED,
                    "Chat session title cannot be empty"
            );
        }

        String normalizedTitle =
                title.trim();

        validateTitleLength(
                normalizedTitle
        );

        return normalizedTitle;
    }

    private void validateTitleLength(
            String title
    ) {
        if (title.length() >
                MAX_TITLE_LENGTH) {

            throw new BadRequestException(
                    ApiErrorCodes.CHAT_TITLE_TOO_LONG,
                    "Chat session title cannot exceed " +
                            MAX_TITLE_LENGTH +
                            " characters"
            );
        }
    }

    /*
     * ---------------------------------------------------------
     * SESSION RESPONSE MAPPING
     * ---------------------------------------------------------
     */
    private ChatSessionResponse toSessionResponse(
            ChatSession session
    ) {
        return new ChatSessionResponse(
                session.getId(),
                session.getWorkspace()
                        .getId(),
                session.getWorkspace()
                        .getName(),
                session.getTitle(),
                session.getCreatedAt(),
                session.getUpdatedAt()
        );
    }

    /*
     * ---------------------------------------------------------
     * MESSAGE RESPONSE MAPPING
     * ---------------------------------------------------------
     *
     * User messages:
     *
     *     sources      = []
     *     explanation  = null
     *
     * Assistant RAG messages:
     *
     *     sources      = persisted source chunks
     *     explanation  = reconstructed from persisted sources
     *                    and retrieval strategy
     */
    private ChatMessageResponse toMessageResponse(
            ChatMessage message
    ) {
        if (message.getSenderType() !=
                SenderType.ASSISTANT) {

            return new ChatMessageResponse(
                    message.getId(),
                    message.getSenderType(),
                    message.getContent(),
                    message.getCreatedAt(),
                    List.of(),
                    null
            );
        }

        List<SemanticSearchResponse> sources =
                chatMessageSourceService
                        .getSources(
                                message.getId()
                        );

        RagExplanationResponse explanation =
                buildExplanation(
                        message,
                        sources
                );

        return new ChatMessageResponse(
                message.getId(),
                message.getSenderType(),
                message.getContent(),
                message.getCreatedAt(),
                sources,
                explanation
        );
    }

    /*
     * ---------------------------------------------------------
     * EXPLAINABLE RAG RECONSTRUCTION
     * ---------------------------------------------------------
     */
    private RagExplanationResponse buildExplanation(
            ChatMessage message,
            List<SemanticSearchResponse> sources
    ) {
        /*
         * Old assistant messages created before Explainable RAG
         * do not have a persisted retrieval strategy.
         *
         * We intentionally do not guess the strategy because:
         *
         * - semantic sources may come from different retrieval modes
         * - broad multi-query and selected-document semantic search
         *   cannot be distinguished reliably after the fact
         *
         * Therefore old messages simply have no explanation.
         */
        if (message.getRetrievalStrategy() == null) {
            return null;
        }

        /*
         * Messages such as:
         *
         * "No documents are attached..."
         *
         * have no RAG evidence and therefore should not expose
         * an explanation.
         */
        if (sources == null ||
                sources.isEmpty()) {

            return null;
        }

        return ragExplanationService.build(
                message.getRetrievalStrategy(),
                sources
        );
    }
}