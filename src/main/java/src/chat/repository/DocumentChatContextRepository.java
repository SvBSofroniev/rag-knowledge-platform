package src.chat.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import src.entity.ChatSession;
import src.entity.Document;
import src.entity.DocumentChatContext;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentChatContextRepository
        extends JpaRepository<DocumentChatContext, UUID> {

    List<DocumentChatContext> findByChatSessionOrderByAttachedAtAsc(
            ChatSession chatSession
    );

    Optional<DocumentChatContext> findByChatSessionAndDocument(
            ChatSession chatSession,
            Document document
    );

    boolean existsByChatSessionAndDocument(
            ChatSession chatSession,
            Document document
    );

    void deleteByChatSessionAndDocument(
            ChatSession chatSession,
            Document document
    );
}