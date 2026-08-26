package src.chat.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import src.entity.ChatSession;
import src.entity.User;
import src.entity.Workspace;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChatSessionRepository
        extends JpaRepository<ChatSession, UUID> {

    List<ChatSession>
    findByWorkspaceAndUserOrderByUpdatedAtDesc(
            Workspace workspace,
            User user
    );

    Optional<ChatSession> findByIdAndUser(
            UUID sessionId,
            User user
    );

    long countByUser(
            User user
    );

    List<ChatSession>
    findTop5ByUserOrderByUpdatedAtDesc(
            User user
    );
}