package src.workspace.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import src.entity.User;
import src.workspace.dto.*;
import src.workspace.service.WorkspaceService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/workspaces")
@RequiredArgsConstructor
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    @PostMapping
    public WorkspaceResponse createWorkspace(
            @RequestBody CreateWorkspaceRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        return workspaceService.createWorkspace(request, currentUser);
    }

    @GetMapping
    public List<WorkspaceResponse> getMyWorkspaces(
            @AuthenticationPrincipal User currentUser
    ) {
        return workspaceService.getMyWorkspaces(currentUser);
    }

    @GetMapping("/{workspaceId}")
    public WorkspaceResponse getWorkspace(
            @PathVariable UUID workspaceId,
            @AuthenticationPrincipal User currentUser
    ) {
        return workspaceService.getWorkspace(workspaceId, currentUser);
    }

    @GetMapping("/{workspaceId}/members")
    public List<WorkspaceMemberResponse> getWorkspaceMembers(
            @PathVariable UUID workspaceId,
            @AuthenticationPrincipal User currentUser
    ) {
        return workspaceService.getWorkspaceMembers(workspaceId, currentUser);
    }

    @PostMapping("/{workspaceId}/members")
    public void addMember(
            @PathVariable UUID workspaceId,
            @RequestBody AddWorkspaceMemberRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        workspaceService.addMember(workspaceId, request, currentUser);
    }

    @PatchMapping("/{workspaceId}/members/{memberId}")
    public void updateMemberRole(
            @PathVariable UUID workspaceId,
            @PathVariable UUID memberId,
            @RequestBody UpdateWorkspaceRoleRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        workspaceService.updateMemberRole(workspaceId, memberId, request, currentUser);
    }

    @DeleteMapping("/{workspaceId}/members/{memberId}")
    public void removeMember(
            @PathVariable UUID workspaceId,
            @PathVariable UUID memberId,
            @AuthenticationPrincipal User currentUser
    ) {
        workspaceService.removeMember(workspaceId, memberId, currentUser);
    }

    @PatchMapping("/{workspaceId}")
    public WorkspaceResponse updateWorkspace(
            @PathVariable UUID workspaceId,
            @Valid @RequestBody UpdateWorkspaceRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        return workspaceService.updateWorkspace(
                workspaceId,
                request,
                currentUser
        );
    }

    @DeleteMapping("/{workspaceId}")
    public void deleteWorkspace(
            @PathVariable UUID workspaceId,
            @AuthenticationPrincipal User currentUser
    ) {
        workspaceService.deleteWorkspace(
                workspaceId,
                currentUser
        );
    }
}