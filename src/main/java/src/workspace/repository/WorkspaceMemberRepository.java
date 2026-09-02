package src.workspace.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import src.entity.User;
import src.entity.Workspace;
import src.entity.WorkspaceMember;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkspaceMemberRepository
        extends JpaRepository<WorkspaceMember, UUID> {

    boolean existsByWorkspaceAndUser(
            Workspace workspace,
            User user
    );

    Optional<WorkspaceMember> findByWorkspaceAndUser(
            Workspace workspace,
            User user
    );

    List<WorkspaceMember> findByUser(
            User user
    );

    List<WorkspaceMember> findByWorkspace(
            Workspace workspace
    );

    long countByWorkspace(
            Workspace workspace
    );
}