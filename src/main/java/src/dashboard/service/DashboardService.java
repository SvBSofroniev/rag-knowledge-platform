package src.dashboard.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import src.chat.repository.ChatSessionRepository;
import src.dashboard.dto.DashboardChatResponse;
import src.dashboard.dto.DashboardDocumentResponse;
import src.dashboard.dto.DashboardResponse;
import src.dashboard.dto.DashboardWorkspaceResponse;
import src.document.repository.DocumentRepository;
import src.document.util.DocumentStatus;
import src.entity.ChatSession;
import src.entity.Document;
import src.entity.User;
import src.entity.Workspace;
import src.entity.WorkspaceMember;
import src.workspace.repository.WorkspaceMemberRepository;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final int RECENT_WORKSPACE_LIMIT = 5;

    private final WorkspaceMemberRepository
            workspaceMemberRepository;

    private final DocumentRepository
            documentRepository;

    private final ChatSessionRepository
            chatSessionRepository;

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard(
            User currentUser
    ) {
        List<WorkspaceMember> memberships =
                workspaceMemberRepository
                        .findByUser(currentUser);

        List<Workspace> workspaces =
                memberships.stream()
                        .map(
                                WorkspaceMember::getWorkspace
                        )
                        .toList();

        long workspaceCount =
                workspaces.size();

        long documentCount;
        long readyDocumentCount;

        List<DashboardDocumentResponse>
                recentDocuments;

        if (workspaces.isEmpty()) {
            documentCount = 0;
            readyDocumentCount = 0;
            recentDocuments = List.of();
        } else {
            documentCount =
                    documentRepository
                            .countByWorkspaceIn(
                                    workspaces
                            );

            readyDocumentCount =
                    documentRepository
                            .countByWorkspaceInAndStatus(
                                    workspaces,
                                    DocumentStatus.READY
                            );

            recentDocuments =
                    documentRepository
                            .findTop5ByWorkspaceInOrderByCreatedAtDesc(
                                    workspaces
                            )
                            .stream()
                            .map(
                                    this::toDocumentResponse
                            )
                            .toList();
        }

        long chatSessionCount =
                chatSessionRepository
                        .countByUser(
                                currentUser
                        );

        List<DashboardChatResponse>
                recentChats =
                chatSessionRepository
                        .findTop5ByUserOrderByUpdatedAtDesc(
                                currentUser
                        )
                        .stream()
                        .map(this::toChatResponse)
                        .toList();

        List<DashboardWorkspaceResponse>
                recentWorkspaces =
                memberships.stream()
                        .sorted(
                                Comparator.comparing(
                                        (
                                                WorkspaceMember member
                                        ) ->
                                                member
                                                        .getWorkspace()
                                                        .getUpdatedAt()
                                ).reversed()
                        )
                        .limit(
                                RECENT_WORKSPACE_LIMIT
                        )
                        .map(
                                this::toWorkspaceResponse
                        )
                        .toList();

        return new DashboardResponse(
                workspaceCount,
                documentCount,
                readyDocumentCount,
                chatSessionCount,
                recentWorkspaces,
                recentDocuments,
                recentChats
        );
    }

    private DashboardWorkspaceResponse
    toWorkspaceResponse(
            WorkspaceMember membership
    ) {
        Workspace workspace =
                membership.getWorkspace();

        long documentCount =
                documentRepository
                        .countByWorkspace(
                                workspace
                        );

        return new DashboardWorkspaceResponse(
                workspace.getId(),
                workspace.getName(),
                workspace.getDescription(),
                membership.getRole(),
                documentCount,
                workspace.getUpdatedAt()
        );
    }

    private DashboardDocumentResponse
    toDocumentResponse(
            Document document
    ) {
        return new DashboardDocumentResponse(
                document.getId(),
                document.getWorkspace().getId(),
                document.getWorkspace().getName(),
                document.getTitle(),
                document.getOriginalFilename(),
                document.getStatus(),
                document.getCreatedAt()
        );
    }

    private DashboardChatResponse
    toChatResponse(
            ChatSession session
    ) {
        return new DashboardChatResponse(
                session.getId(),
                session.getWorkspace().getId(),
                session.getWorkspace().getName(),
                session.getTitle(),
                session.getUpdatedAt()
        );
    }
}