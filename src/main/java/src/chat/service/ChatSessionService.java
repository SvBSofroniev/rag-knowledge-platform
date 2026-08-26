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
import src.common.exception.BadRequestException;
import src.common.exception.ResourceNotFoundException;
import src.entity.ChatMessage;
import src.entity.ChatSession;
import src.entity.User;
import src.entity.Workspace;
import src.entity.WorkspaceMember;
import src.rag.dto.SemanticSearchResponse;
import src.util.SenderType;
import src.workspace.service.WorkspacePermissionService;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatSessionService {

    private static final String DEFAULT_TITLE = "New chat";
    private static final int MAX_TITLE_LENGTH = 255;

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final WorkspacePermissionService workspacePermissionService;
    private final ChatMessageSourceService chatMessageSourceService;

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

        Workspace workspace = membership.getWorkspace();

        ChatSession session = new ChatSession();
        session.setWorkspace(workspace);
        session.setUser(currentUser);
        session.setTitle(resolveTitle(request));

        ChatSession savedSession =
                chatSessionRepository.save(session);

        return toSessionResponse(savedSession);
    }

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

        Workspace workspace = membership.getWorkspace();

        return chatSessionRepository
                .findByWorkspaceAndUserOrderByUpdatedAtDesc(
                        workspace,
                        currentUser
                )
                .stream()
                .map(this::toSessionResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ChatSessionResponse getSession(
            UUID sessionId,
            User currentUser
    ) {
        ChatSession session =
                getAccessibleSession(sessionId, currentUser);

        return toSessionResponse(session);
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getSessionMessages(
            UUID sessionId,
            User currentUser
    ) {
        ChatSession session =
                getAccessibleSession(sessionId, currentUser);

        return chatMessageRepository
                .findBySessionOrderByCreatedAtAsc(session)
                .stream()
                .map(message -> {
                    List<SemanticSearchResponse> sources =
                            message.getSenderType() == SenderType.ASSISTANT
                                    ? chatMessageSourceService.getSources(
                                    message.getId()
                            )
                                    : List.of();

                    return toMessageResponse(
                            message,
                            sources
                    );
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public ChatSession getAccessibleSession(
            UUID sessionId,
            User currentUser
    ) {
        ChatSession session = chatSessionRepository
                .findById(sessionId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Chat session not found"
                        )
                );

        /*
         * First verify that the current user still belongs
         * to the workspace.
         */
        workspacePermissionService.requireMember(
                session.getWorkspace().getId(),
                currentUser
        );

        /*
         * Chat sessions are personal. A workspace member must
         * not access another member's session by guessing its UUID.
         *
         * Return 404 instead of 403 to avoid revealing that another
         * user's chat session exists.
         */
        if (!session.getUser()
                .getId()
                .equals(currentUser.getId())) {
            throw new ResourceNotFoundException(
                    "Chat session not found"
            );
        }

        return session;
    }

    @Transactional
    public ChatSessionResponse updateSession(
            UUID sessionId,
            UpdateChatSessionRequest request,
            User currentUser
    ) {
        String normalizedTitle =
                validateAndNormalizeUpdateTitle(request);

        ChatSession session =
                getAccessibleSession(sessionId, currentUser);

        session.setTitle(normalizedTitle);

        /*
         * Explicit save is valid, although Hibernate dirty checking
         * would also persist the changed managed entity.
         */
        ChatSession updatedSession =
                chatSessionRepository.save(session);

        return toSessionResponse(updatedSession);
    }

    @Transactional
    public void deleteSession(
            UUID sessionId,
            User currentUser
    ) {
        ChatSession session =
                getAccessibleSession(sessionId, currentUser);

        chatSessionRepository.delete(session);
    }

    private String resolveTitle(
            CreateChatSessionRequest request
    ) {
        if (request == null ||
                request.title() == null ||
                request.title().isBlank()) {
            return DEFAULT_TITLE;
        }

        String normalizedTitle = request.title().trim();

        validateTitleLength(normalizedTitle);

        return normalizedTitle;
    }

    private String validateAndNormalizeUpdateTitle(
            UpdateChatSessionRequest request
    ) {
        if (request == null) {
            throw new BadRequestException(
                    "Chat session update request cannot be empty"
            );
        }

        String title = request.title();

        if (title == null || title.isBlank()) {
            throw new BadRequestException(
                    "Chat session title cannot be empty"
            );
        }

        String normalizedTitle = title.trim();

        validateTitleLength(normalizedTitle);

        return normalizedTitle;
    }

    private void validateTitleLength(String title) {
        if (title.length() > MAX_TITLE_LENGTH) {
            throw new BadRequestException(
                    "Chat session title cannot exceed " +
                            MAX_TITLE_LENGTH +
                            " characters"
            );
        }
    }

    private ChatSessionResponse toSessionResponse(
            ChatSession session
    ) {
        return new ChatSessionResponse(
                session.getId(),
                session.getWorkspace().getId(),
                session.getWorkspace().getName(),
                session.getTitle(),
                session.getCreatedAt(),
                session.getUpdatedAt()
        );
    }

    private ChatMessageResponse toMessageResponse(
            ChatMessage message,
            List<SemanticSearchResponse> sources
    ) {
        return new ChatMessageResponse(
                message.getId(),
                message.getSenderType(),
                message.getContent(),
                message.getCreatedAt(),
                sources
        );
    }
}