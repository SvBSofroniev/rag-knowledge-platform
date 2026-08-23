package src.workspace.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import src.auth.repository.UserRepository;
import src.common.exception.BadRequestException;
import src.common.exception.ConflictException;
import src.common.exception.ForbiddenOperationException;
import src.common.exception.ResourceNotFoundException;
import src.entity.User;
import src.entity.Workspace;
import src.entity.WorkspaceMember;
import src.workspace.dto.*;

import src.workspace.repository.WorkspaceMemberRepository;
import src.workspace.repository.WorkspaceRepository;
import src.workspace.util.WorkspaceRole;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final WorkspacePermissionService permissionService;

    @Transactional
    public WorkspaceResponse createWorkspace(CreateWorkspaceRequest request, User currentUser) {
        Workspace workspace = new Workspace();
        workspace.setName(request.name());
        workspace.setDescription(request.description());
        workspace.setCreatedBy(currentUser);

        WorkspaceMember owner = new WorkspaceMember();
        owner.setWorkspace(workspace);
        owner.setUser(currentUser);
        owner.setRole(WorkspaceRole.OWNER);

        workspace.getMembers().add(owner);

        Workspace saved = workspaceRepository.save(workspace);

        return new WorkspaceResponse(
                saved.getId(),
                saved.getName(),
                saved.getDescription(),
                WorkspaceRole.OWNER
        );
    }

    public List<WorkspaceResponse> getMyWorkspaces(User currentUser) {
        return memberRepository.findByUser(currentUser)
                .stream()
                .map(member -> new WorkspaceResponse(
                        member.getWorkspace().getId(),
                        member.getWorkspace().getName(),
                        member.getWorkspace().getDescription(),
                        member.getRole()
                ))
                .toList();
    }

    public WorkspaceResponse getWorkspace(UUID workspaceId, User currentUser) {
        WorkspaceMember member = permissionService.requireMember(workspaceId, currentUser);
        Workspace workspace = member.getWorkspace();

        return new WorkspaceResponse(
                workspace.getId(),
                workspace.getName(),
                workspace.getDescription(),
                member.getRole()
        );
    }

    @Transactional
    public void addMember(UUID workspaceId, AddWorkspaceMemberRequest request, User currentUser) {
        WorkspaceMember currentMember = permissionService.requireAdminOrOwner(workspaceId, currentUser);
        Workspace workspace = currentMember.getWorkspace();

        User userToAdd = userRepository.findById(request.userId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found")
                );

        if (memberRepository.existsByWorkspaceAndUser(
                workspace,
                userToAdd
        )) {
            throw new ConflictException(
                    "User is already a workspace member"
            );
        }

        WorkspaceMember newMember = new WorkspaceMember();
        newMember.setWorkspace(workspace);
        newMember.setUser(userToAdd);
        newMember.setRole(WorkspaceRole.MEMBER);

        memberRepository.save(newMember);
    }

    @Transactional
    public void updateMemberRole(
            UUID workspaceId,
            UUID memberId,
            UpdateWorkspaceRoleRequest request,
            User currentUser
    ) {
        permissionService.requireOwner(workspaceId, currentUser);

        WorkspaceMember targetMember =
                memberRepository.findById(memberId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Workspace member not found"
                                )
                        );

        if (!targetMember.getWorkspace()
                .getId()
                .equals(workspaceId)) {
            throw new ResourceNotFoundException(
                    "Workspace member not found"
            );
        }

        if (targetMember.getRole() == WorkspaceRole.OWNER) {
            throw new ForbiddenOperationException(
                    "The workspace owner's role cannot be changed"
            );
        }

        if (request.role() == WorkspaceRole.OWNER) {
            throw new BadRequestException(
                    "The OWNER role cannot be assigned through this operation"
            );
        }

        if (targetMember.getRole() == request.role()) {
            throw new ConflictException(
                    "The member already has this role"
            );
        }

        targetMember.setRole(request.role());
        memberRepository.save(targetMember);
    }

    @Transactional
    public void removeMember(UUID workspaceId, UUID memberId, User currentUser) {
        permissionService.requireAdminOrOwner(workspaceId, currentUser);

        WorkspaceMember targetMember =
                memberRepository.findById(memberId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Workspace member not found"
                                )
                        );

        if (!targetMember.getWorkspace()
                .getId()
                .equals(workspaceId)) {
            throw new ResourceNotFoundException(
                    "Workspace member not found"
            );
        }

        if (targetMember.getRole() == WorkspaceRole.OWNER) {
            throw new ForbiddenOperationException(
                    "The workspace owner cannot be removed"
            );
        }

        memberRepository.delete(targetMember);
    }

    public List<WorkspaceMemberResponse> getWorkspaceMembers(
            UUID workspaceId,
            User currentUser
    ) {
        WorkspaceMember currentMember = permissionService.requireMember(workspaceId, currentUser);
        Workspace workspace = currentMember.getWorkspace();

        return memberRepository.findByWorkspace(workspace)
                .stream()
                .map(member -> new WorkspaceMemberResponse(
                        member.getId(),
                        member.getUser().getId(),
                        member.getUser().getUsername(),
                        member.getUser().getEmail(),
                        member.getRole(),
                        member.getJoinedAt()
                ))
                .toList();
    }
}