package src.workspace.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import src.auth.repository.UserRepository;
import src.entity.User;
import src.entity.Workspace;
import src.entity.WorkspaceMember;
import src.workspace.dto.*;

import src.workspace.repository.WorkspaceMemberRepository;
import src.workspace.repository.WorkspaceRepository;
import src.workspace.util.WorkspaceRole;

import java.util.List;

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

    public WorkspaceResponse getWorkspace(String workspaceId, User currentUser) {
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
    public void addMember(String workspaceId, AddWorkspaceMemberRequest request, User currentUser) {
        WorkspaceMember currentMember = permissionService.requireAdminOrOwner(workspaceId, currentUser);
        Workspace workspace = currentMember.getWorkspace();

        User userToAdd = userRepository.findById(request.userId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (memberRepository.existsByWorkspaceAndUser(workspace, userToAdd)) {
            throw new RuntimeException("User is already a member");
        }

        WorkspaceMember newMember = new WorkspaceMember();
        newMember.setWorkspace(workspace);
        newMember.setUser(userToAdd);
        newMember.setRole(WorkspaceRole.MEMBER);

        memberRepository.save(newMember);
    }

    @Transactional
    public void updateMemberRole(
            String workspaceId,
            String memberId,
            UpdateWorkspaceRoleRequest request,
            User currentUser
    ) {
        permissionService.requireOwner(workspaceId, currentUser);

        WorkspaceMember targetMember = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));

        if (!targetMember.getWorkspace().getId().equals(workspaceId)) {
            throw new RuntimeException("Member does not belong to this workspace");
        }

        if (targetMember.getRole() == WorkspaceRole.OWNER) {
            throw new RuntimeException("Cannot change owner role");
        }

        if (request.role() == WorkspaceRole.OWNER) {
            throw new RuntimeException("Cannot assign OWNER role here");
        }

        targetMember.setRole(request.role());
        memberRepository.save(targetMember);
    }

    @Transactional
    public void removeMember(String workspaceId, String memberId, User currentUser) {
        permissionService.requireAdminOrOwner(workspaceId, currentUser);

        WorkspaceMember targetMember = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));

        if (!targetMember.getWorkspace().getId().equals(workspaceId)) {
            throw new RuntimeException("Member does not belong to this workspace");
        }

        if (targetMember.getRole() == WorkspaceRole.OWNER) {
            throw new RuntimeException("Cannot remove workspace owner");
        }

        memberRepository.delete(targetMember);
    }

    public List<WorkspaceMemberResponse> getWorkspaceMembers(
            String workspaceId,
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