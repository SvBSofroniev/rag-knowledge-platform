package src.chat.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import src.entity.ChatMessage;
import src.entity.ChatSession;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChatMessageRepository
        extends JpaRepository<ChatMessage, UUID> {

    List<ChatMessage> findBySessionOrderByCreatedAtAsc(
            ChatSession session
    );

    List<ChatMessage> findTop10BySessionOrderByCreatedAtDesc(
            ChatSession session
    );

    Optional<ChatMessage> findByIdAndSession(
            UUID messageId,
            ChatSession session
    );
}