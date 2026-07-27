package src.chat.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import src.chat.dto.AttachedDocumentResponse;
import src.chat.repository.DocumentChatContextRepository;
import src.document.repository.DocumentRepository;
import src.document.util.DocumentStatus;
import src.entity.ChatSession;
import src.entity.Document;
import src.entity.DocumentChatContext;
import src.entity.User;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatDocumentContextService {

    private final ChatSessionService chatSessionService;
    private final DocumentRepository documentRepository;
    private final DocumentChatContextRepository contextRepository;

    @Transactional
    public AttachedDocumentResponse attachDocument(
            UUID sessionId,
            UUID documentId,
            User currentUser
    ) {
        ChatSession session = chatSessionService.getAccessibleSession(
                sessionId,
                currentUser
        );

        Document document = getDocument(documentId);

        validateSameWorkspace(session, document);

        if (document.getStatus() != DocumentStatus.READY) {
            throw new RuntimeException(
                    "Only READY documents can be attached to a chat session"
            );
        }

        if (contextRepository.existsByChatSessionAndDocument(
                session,
                document
        )) {
            throw new RuntimeException(
                    "Document is already attached to this chat session"
            );
        }

        DocumentChatContext context = new DocumentChatContext();
        context.setChatSession(session);
        context.setDocument(document);

        DocumentChatContext saved = contextRepository.save(context);

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<AttachedDocumentResponse> getAttachedDocuments(
            UUID sessionId,
            User currentUser
    ) {
        ChatSession session = chatSessionService.getAccessibleSession(
                sessionId,
                currentUser
        );

        return contextRepository
                .findByChatSessionOrderByAttachedAtAsc(session)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void detachDocument(
            UUID sessionId,
            UUID documentId,
            User currentUser
    ) {
        ChatSession session = chatSessionService.getAccessibleSession(
                sessionId,
                currentUser
        );

        Document document = getDocument(documentId);

        validateSameWorkspace(session, document);

        DocumentChatContext context = contextRepository
                .findByChatSessionAndDocument(session, document)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Document is not attached to this chat session"
                        )
                );

        contextRepository.delete(context);
    }

    @Transactional(readOnly = true)
    public List<UUID> getAttachedDocumentIds(
            ChatSession session
    ) {
        return contextRepository
                .findByChatSessionOrderByAttachedAtAsc(session)
                .stream()
                .map(context -> context.getDocument().getId())
                .toList();
    }

    private Document getDocument(UUID documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() ->
                        new RuntimeException("Document not found")
                );
    }

    private void validateSameWorkspace(
            ChatSession session,
            Document document
    ) {
        UUID sessionWorkspaceId =
                session.getWorkspace().getId();

        UUID documentWorkspaceId =
                document.getWorkspace().getId();

        if (!sessionWorkspaceId.equals(documentWorkspaceId)) {
            throw new RuntimeException(
                    "Document does not belong to the chat session workspace"
            );
        }
    }

    private AttachedDocumentResponse toResponse(
            DocumentChatContext context
    ) {
        Document document = context.getDocument();

        return new AttachedDocumentResponse(
                context.getId(),
                document.getId(),
                document.getTitle(),
                document.getOriginalFilename(),
                document.getStatus(),
                context.getAttachedAt()
        );
    }
}