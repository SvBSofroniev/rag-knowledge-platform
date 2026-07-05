package src.workspace.dto;

import java.util.UUID;

public record AddWorkspaceMemberRequest(
        UUID userId
) {}
