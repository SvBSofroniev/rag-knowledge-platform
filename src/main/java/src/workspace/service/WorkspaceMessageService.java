package src.workspace.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import src.common.exception.ApiErrorCodes;
import src.common.exception.BadRequestException;
import src.entity.User;
import src.entity.Workspace;
import src.workspace.dto.SendWorkspaceMessageRequest;
import src.workspace.dto.WorkspaceMessageResponse;
import src.workspace.entity.WorkspaceMessage;
import src.workspace.repository.WorkspaceMessageRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WorkspaceMessageService {

    private static final int
            MAX_MESSAGE_LENGTH =
            2_000;

    private final WorkspaceMessageRepository
            workspaceMessageRepository;

    private final WorkspacePermissionService
            workspacePermissionService;

    @Transactional(readOnly = true)
    public List<WorkspaceMessageResponse>
    getMessages(
            UUID workspaceId,
            User currentUser
    ) {
        Workspace workspace =
                workspacePermissionService
                        .requireMember(
                                workspaceId,
                                currentUser
                        )
                        .getWorkspace();

        List<WorkspaceMessage> messages =
                new ArrayList<>(
                        workspaceMessageRepository
                                .findTop100ByWorkspaceOrderByCreatedAtDesc(
                                        workspace
                                )
                );

        /*
         * Repository gives newest-first.
         * Chat UI should receive oldest-first.
         */
        Collections.reverse(
                messages
        );

        return messages
                .stream()
                .map(message ->
                        toResponse(
                                message,
                                currentUser
                        )
                )
                .toList();
    }

    @Transactional
    public WorkspaceMessageResponse
    sendMessage(
            UUID workspaceId,
            SendWorkspaceMessageRequest request,
            User currentUser
    ) {
        validateRequest(
                request
        );

        Workspace workspace =
                workspacePermissionService
                        .requireMember(
                                workspaceId,
                                currentUser
                        )
                        .getWorkspace();

        String content =
                request
                        .content()
                        .trim();

        WorkspaceMessage message =
                new WorkspaceMessage();

        message.setWorkspace(
                workspace
        );

        message.setUser(
                currentUser
        );

        message.setContent(
                content
        );

        WorkspaceMessage saved =
                workspaceMessageRepository
                        .save(
                                message
                        );

        return toResponse(
                saved,
                currentUser
        );
    }

    private void validateRequest(
            SendWorkspaceMessageRequest request
    ) {
        if (request == null ||
                request.content() == null ||
                request.content().isBlank()) {

            throw new BadRequestException(
                    ApiErrorCodes.WORKSPACE_MESSAGE_REQUIRED,
                    "Workspace message cannot be empty"
            );
        }

        if (request
                .content()
                .trim()
                .length() >
                MAX_MESSAGE_LENGTH) {

            throw new BadRequestException(
                    ApiErrorCodes.WORKSPACE_MESSAGE_TOO_LONG,
                    "Workspace message cannot exceed " +
                            MAX_MESSAGE_LENGTH +
                            " characters"
            );
        }
    }

    private WorkspaceMessageResponse
    toResponse(
            WorkspaceMessage message,
            User currentUser
    ) {
        User sender =
                message.getUser();

        return new WorkspaceMessageResponse(
                message.getId(),
                sender.getId(),
                sender.getUsername(),
                resolveDisplayName(
                        sender
                ),
                message.getContent(),
                message.getCreatedAt(),
                sender.getId()
                        .equals(
                                currentUser.getId()
                        )
        );
    }

    private String resolveDisplayName(
            User user
    ) {
        String firstName =
                user.getFirstName();

        String lastName =
                user.getLastName();

        String fullName =
                (
                        (firstName == null
                                ? ""
                                : firstName.trim()) +
                                " " +
                                (lastName == null
                                        ? ""
                                        : lastName.trim())
                ).trim();

        if (!fullName.isBlank()) {
            return fullName;
        }

        return user.getUsername();
    }
}