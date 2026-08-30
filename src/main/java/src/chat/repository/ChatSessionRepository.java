package src.chat.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Query("""
        SELECT session
        FROM ChatSession session
        WHERE session.user = :user
          AND EXISTS (
              SELECT member
              FROM WorkspaceMember member
              WHERE member.workspace = session.workspace
                AND member.user = :user
          )
        ORDER BY session.updatedAt DESC
        """)
    List<ChatSession> findAllAccessibleByUser(
            @Param("user") User user
    );
}