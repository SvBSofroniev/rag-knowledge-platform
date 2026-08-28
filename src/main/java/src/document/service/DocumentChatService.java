package src.document.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import src.chat.dto.ChatSessionResponse;
import src.chat.dto.CreateChatSessionRequest;
import src.chat.repository.DocumentChatContextRepository;
import src.chat.service.ChatDocumentContextService;
import src.chat.service.ChatSessionService;
import src.common.exception.ConflictException;
import src.document.util.DocumentStatus;
import src.entity.Document;
import src.entity.DocumentChatContext;
import src.entity.User;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentChatService {

    private final DocumentService documentService;
    private final ChatSessionService chatSessionService;
    private final ChatDocumentContextService chatDocumentContextService;
    private final DocumentChatContextRepository contextRepository;

    @Transactional
    public ChatSessionResponse createChatForDocument(
            UUID documentId,
            User currentUser
    ) {
        Document document =
                documentService.getDocumentOrThrow(
                        documentId
                );

        documentService.requireDocumentAccess(
                document,
                currentUser
        );

        if (document.getStatus() != DocumentStatus.READY) {
            throw new ConflictException(
                    "Only READY documents can be used to start a chat"
            );
        }

        /*
         * First try to find the most recently used chat
         * belonging to the current user where this document
         * is already attached.
         */
        var existingContext =
                contextRepository
                        .findFirstByDocumentAndChatSession_UserOrderByChatSession_UpdatedAtDesc(
                                document,
                                currentUser
                        );

        if (existingContext.isPresent()) {
            DocumentChatContext context =
                    existingContext.get();

            return chatSessionService.getSession(
                    context.getChatSession().getId(),
                    currentUser
            );
        }

        /*
         * No existing personal chat for this document.
         * Create one and attach the document.
         */
        UUID workspaceId =
                document.getWorkspace()
                        .getId();

        CreateChatSessionRequest request =
                new CreateChatSessionRequest(
                        resolveChatTitle(
                                document
                        )
                );

        ChatSessionResponse session =
                chatSessionService.createSession(
                        workspaceId,
                        request,
                        currentUser
                );

        chatDocumentContextService.attachDocument(
                session.id(),
                document.getId(),
                currentUser
        );

        return session;
    }

    private String resolveChatTitle(
            Document document
    ) {
        String title =
                document.getTitle();

        if (title == null ||
                title.isBlank()) {

            return "Document chat";
        }

        String normalized =
                title.trim();

        /*
         * ChatSessionService accepts max 255 characters.
         */
        if (normalized.length() <= 255) {
            return normalized;
        }

        return normalized.substring(
                0,
                255
        );
    }
}