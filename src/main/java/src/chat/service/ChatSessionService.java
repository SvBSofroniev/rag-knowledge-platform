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
import src.entity.ChatMessage;
import src.entity.ChatSession;
import src.entity.User;
import src.entity.Workspace;
import src.entity.WorkspaceMember;
import src.workspace.service.WorkspacePermissionService;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatSessionService {

    private static final String DEFAULT_TITLE = "New chat";

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final WorkspacePermissionService workspacePermissionService;

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
                .map(this::toMessageResponse)
                .toList();
    }

    public ChatSession getAccessibleSession(
            UUID sessionId,
            User currentUser
    ) {
        ChatSession session = chatSessionRepository
                .findByIdAndUser(sessionId, currentUser)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Chat session not found"
                        )
                );

        /*
         * A user may own the session but could have later been
         * removed from the workspace.
         */
        workspacePermissionService.requireMember(
                session.getWorkspace().getId(),
                currentUser
        );

        return session;
    }

    private String resolveTitle(
            CreateChatSessionRequest request
    ) {
        if (request == null ||
                request.title() == null ||
                request.title().isBlank()) {
            return DEFAULT_TITLE;
        }

        return request.title().trim();
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
            ChatMessage message
    ) {
        return new ChatMessageResponse(
                message.getId(),
                message.getSenderType(),
                message.getContent(),
                message.getCreatedAt()
        );
    }

    @Transactional
    public ChatSessionResponse updateSession(
            UUID sessionId,
            UpdateChatSessionRequest request,
            User currentUser
    ) {
        ChatSession session = getAccessibleSession(
                sessionId,
                currentUser
        );

        session.setTitle(request.title().trim());

        ChatSession updatedSession =
                chatSessionRepository.save(session);

        return toSessionResponse(updatedSession);
    }

    @Transactional
    public void deleteSession(
            UUID sessionId,
            User currentUser
    ) {
        ChatSession session = getAccessibleSession(
                sessionId,
                currentUser
        );

        chatSessionRepository.delete(session);
    }
}