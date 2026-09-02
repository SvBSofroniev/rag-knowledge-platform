package src.workspace.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import src.entity.Workspace;
import src.workspace.entity.WorkspaceMessage;

import java.util.List;
import java.util.UUID;

@Repository
public interface WorkspaceMessageRepository
        extends JpaRepository<
        WorkspaceMessage,
        UUID
        > {

    List<WorkspaceMessage>
    findTop100ByWorkspaceOrderByCreatedAtDesc(
            Workspace workspace
    );
}