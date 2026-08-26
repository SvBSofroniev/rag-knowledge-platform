package src.dashboard.dto;

import java.util.List;

public record DashboardResponse(
        long workspaceCount,
        long documentCount,
        long readyDocumentCount,
        long chatSessionCount,
        List<DashboardWorkspaceResponse> recentWorkspaces,
        List<DashboardDocumentResponse> recentDocuments,
        List<DashboardChatResponse> recentChats
) {
}