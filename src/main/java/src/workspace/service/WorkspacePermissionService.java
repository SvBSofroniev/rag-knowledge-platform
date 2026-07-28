package src.workspace.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import src.common.exception.ForbiddenOperationException;
import src.common.exception.ResourceNotFoundException;
import src.entity.User;
import src.entity.Workspace;
import src.entity.WorkspaceMember;
import src.workspace.repository.WorkspaceMemberRepository;
import src.workspace.repository.WorkspaceRepository;
import src.workspace.util.WorkspaceRole;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WorkspacePermissionService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository memberRepository;

    public Workspace getWorkspaceOrThrow(UUID workspaceId) {
        return workspaceRepository.findById(workspaceId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Workspace not found"
                        )
                );
    }

    public WorkspaceMember getMemberOrThrow(
            Workspace workspace,
            User user
    ) {
        return memberRepository
                .findByWorkspaceAndUser(workspace, user)
                .orElseThrow(() ->
                        new ForbiddenOperationException(
                                "You are not a member of this workspace"
                        )
                );
    }

    public WorkspaceMember requireMember(
            UUID workspaceId,
            User user
    ) {
        Workspace workspace =
                getWorkspaceOrThrow(workspaceId);

        return getMemberOrThrow(workspace, user);
    }

    public WorkspaceMember requireAdminOrOwner(
            UUID workspaceId,
            User user
    ) {
        WorkspaceMember member =
                requireMember(workspaceId, user);

        if (member.getRole() != WorkspaceRole.ADMIN &&
                member.getRole() != WorkspaceRole.OWNER) {
            throw new ForbiddenOperationException(
                    "Only workspace administrators or the owner can perform this action"
            );
        }

        return member;
    }

    public WorkspaceMember requireOwner(
            UUID workspaceId,
            User user
    ) {
        WorkspaceMember member =
                requireMember(workspaceId, user);

        if (member.getRole() != WorkspaceRole.OWNER) {
            throw new ForbiddenOperationException(
                    "Only the workspace owner can perform this action"
            );
        }

        return member;
    }

    public boolean isMember(
            UUID workspaceId,
            User user
    ) {
        Workspace workspace =
                getWorkspaceOrThrow(workspaceId);

        return memberRepository
                .existsByWorkspaceAndUser(workspace, user);
    }
}