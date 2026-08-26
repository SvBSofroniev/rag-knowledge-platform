package src.chat.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import src.entity.ChatMessageSource;

import java.util.List;
import java.util.UUID;

public interface ChatMessageSourceRepository
        extends JpaRepository<ChatMessageSource, UUID> {

    List<ChatMessageSource>
    findAllByMessageIdOrderBySourceRankAsc(
            UUID messageId
    );
}