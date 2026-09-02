package src.workspace.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import src.entity.User;
import src.workspace.dto.SendWorkspaceMessageRequest;
import src.workspace.dto.WorkspaceMessageResponse;
import src.workspace.service.WorkspaceMessageService;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping(
        "/api/workspaces/{workspaceId}/messages"
)
public class WorkspaceMessageController {

    private final WorkspaceMessageService
            workspaceMessageService;

    @GetMapping
    public List<WorkspaceMessageResponse>
    getMessages(
            @PathVariable UUID workspaceId,
            @AuthenticationPrincipal User currentUser
    ) {
        return workspaceMessageService
                .getMessages(
                        workspaceId,
                        currentUser
                );
    }

    @PostMapping
    public WorkspaceMessageResponse
    sendMessage(
            @PathVariable UUID workspaceId,
            @Valid
            @RequestBody
            SendWorkspaceMessageRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        return workspaceMessageService
                .sendMessage(
                        workspaceId,
                        request,
                        currentUser
                );
    }
}