package src.workspace.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import src.entity.User;
import src.workspace.dto.*;
import src.workspace.service.WorkspaceService;

import java.util.List;

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
            @PathVariable String workspaceId,
            @AuthenticationPrincipal User currentUser
    ) {
        return workspaceService.getWorkspace(workspaceId, currentUser);
    }

    @GetMapping("/{workspaceId}/members")
    public List<WorkspaceMemberResponse> getWorkspaceMembers(
            @PathVariable String workspaceId,
            @AuthenticationPrincipal User currentUser
    ) {
        return workspaceService.getWorkspaceMembers(workspaceId, currentUser);
    }

    @PostMapping("/{workspaceId}/members")
    public void addMember(
            @PathVariable String workspaceId,
            @RequestBody AddWorkspaceMemberRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        workspaceService.addMember(workspaceId, request, currentUser);
    }

    @PatchMapping("/{workspaceId}/members/{memberId}")
    public void updateMemberRole(
            @PathVariable String workspaceId,
            @PathVariable String memberId,
            @RequestBody UpdateWorkspaceRoleRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        workspaceService.updateMemberRole(workspaceId, memberId, request, currentUser);
    }

    @DeleteMapping("/{workspaceId}/members/{memberId}")
    public void removeMember(
            @PathVariable String workspaceId,
            @PathVariable String memberId,
            @AuthenticationPrincipal User currentUser
    ) {
        workspaceService.removeMember(workspaceId, memberId, currentUser);
    }
}