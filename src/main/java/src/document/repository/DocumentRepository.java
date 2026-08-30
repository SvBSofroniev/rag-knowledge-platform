package src.document.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import src.entity.Document;
import src.entity.User;
import src.entity.Workspace;
import src.document.util.DocumentStatus;

import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentRepository
        extends JpaRepository<Document, UUID> {

    List<Document> findByWorkspaceOrderByCreatedAtDesc(
            Workspace workspace
    );

    long countByWorkspace(
            Workspace workspace
    );

    long countByWorkspaceIn(
            List<Workspace> workspaces
    );

    long countByWorkspaceInAndStatus(
            List<Workspace> workspaces,
            DocumentStatus status
    );

    List<Document>
    findTop5ByWorkspaceInOrderByCreatedAtDesc(
            List<Workspace> workspaces
    );

    @Query("""
        SELECT d
        FROM Document d
        JOIN d.workspace.members wm
        WHERE wm.user = :user
        ORDER BY d.createdAt DESC
        """)
    List<Document> findAllAccessibleByUser(
            @Param("user") User user
    );
}