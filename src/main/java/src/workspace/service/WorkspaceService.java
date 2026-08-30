package src.workspace.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import src.auth.repository.UserRepository;
import src.common.exception.*;
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
                        new ResourceNotFoundException(
                                ApiErrorCodes.USER_NOT_FOUND,
                                "User not found"
                        )
                );

        if (memberRepository.existsByWorkspaceAndUser(
                workspace,
                userToAdd
        )) {
            throw new ConflictException(
                    ApiErrorCodes.WORKSPACE_MEMBER_ALREADY_EXISTS,
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
                                        ApiErrorCodes.WORKSPACE_MEMBER_NOT_FOUND,
                                        "Workspace member not found"
                                )
                        );

        if (!targetMember.getWorkspace()
                .getId()
                .equals(workspaceId)) {
            throw new ResourceNotFoundException(
                    ApiErrorCodes.WORKSPACE_MEMBER_NOT_FOUND,
                    "Workspace member not found"
            );
        }

        if (targetMember.getRole() == WorkspaceRole.OWNER) {
            throw new ForbiddenOperationException(
                    ApiErrorCodes.WORKSPACE_OWNER_ROLE_IMMUTABLE,
                    "The workspace owner's role cannot be changed"
            );
        }

        if (request.role() == WorkspaceRole.OWNER) {
            throw new BadRequestException(
                    ApiErrorCodes.WORKSPACE_OWNER_ASSIGNMENT_FORBIDDEN,
                    "The OWNER role cannot be assigned through this operation"
            );
        }

        if (targetMember.getRole() == request.role()) {
            throw new ConflictException(
                    ApiErrorCodes.WORKSPACE_ROLE_UNCHANGED,
                    "The member already has this role"
            );
        }

        targetMember.setRole(request.role());
        memberRepository.save(targetMember);
    }

    @Transactional
    public void removeMember(
            UUID workspaceId,
            UUID memberId,
            User currentUser
    ) {
        WorkspaceMember currentMember =
                permissionService
                        .requireAdminOrOwner(
                                workspaceId,
                                currentUser
                        );

        WorkspaceMember targetMember =
                memberRepository
                        .findById(memberId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        ApiErrorCodes.WORKSPACE_MEMBER_NOT_FOUND,
                                        "Workspace member not found"
                                )
                        );

        /*
         * Prevent using a member ID that belongs
         * to another workspace.
         */
        if (!targetMember
                .getWorkspace()
                .getId()
                .equals(workspaceId)) {

            throw new ResourceNotFoundException(
                    ApiErrorCodes.WORKSPACE_MEMBER_NOT_FOUND,
                    "Workspace member not found"
            );
        }

        /*
         * Ownership cannot be removed.
         */
        if (
                targetMember.getRole() ==
                        WorkspaceRole.OWNER
        ) {
            throw new ForbiddenOperationException(
                    ApiErrorCodes.ADMIN_CANNOT_REMOVE_ADMIN,
                    "Only the workspace owner can remove an administrator"
            );
        }

        /*
         * ADMIN can remove MEMBER only.
         *
         * Only OWNER can remove another ADMIN.
         */
        if (
                currentMember.getRole() ==
                        WorkspaceRole.ADMIN &&
                        targetMember.getRole() ==
                                WorkspaceRole.ADMIN
        ) {
            throw new ForbiddenOperationException(
                    ApiErrorCodes.ADMIN_CANNOT_REMOVE_ADMIN,
                    "Only the workspace owner can remove an administrator"
            );
        }

        memberRepository.delete(
                targetMember
        );
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

    @Transactional
    public WorkspaceResponse updateWorkspace(
            UUID workspaceId,
            UpdateWorkspaceRequest request,
            User currentUser
    ) {
        WorkspaceMember currentMember =
                permissionService.requireAdminOrOwner(
                        workspaceId,
                        currentUser
                );

        Workspace workspace =
                currentMember.getWorkspace();

        String name = request.name().trim();

        if (name.isEmpty()) {
            throw new BadRequestException(
                    ApiErrorCodes.WORKSPACE_NAME_REQUIRED,
                    "Workspace name cannot be empty"
            );
        }

        String description =
                request.description() == null
                        ? null
                        : request.description().trim();

        if (description != null &&
                description.isEmpty()) {
            description = null;
        }

        workspace.setName(name);
        workspace.setDescription(description);

        Workspace saved =
                workspaceRepository.save(workspace);

        return new WorkspaceResponse(
                saved.getId(),
                saved.getName(),
                saved.getDescription(),
                currentMember.getRole()
        );
    }

    @Transactional
    public void deleteWorkspace(
            UUID workspaceId,
            User currentUser
    ) {
        WorkspaceMember owner =
                permissionService.requireOwner(
                        workspaceId,
                        currentUser
                );

        Workspace workspace =
                owner.getWorkspace();

        workspaceRepository.delete(workspace);
    }

}