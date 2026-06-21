package src.entity.embeddable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class WorkspaceMemberId implements Serializable {

    @Column(name = "workspace_id")
    private UUID workspaceId;

    @Column(name = "user_id")
    private UUID userId;

    public WorkspaceMemberId() {}

    public WorkspaceMemberId(UUID workspaceId, UUID userId) {
        this.workspaceId = workspaceId;
        this.userId = userId;
    }

    // getters and setters

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WorkspaceMemberId that)) return false;
        return Objects.equals(workspaceId, that.workspaceId)
                && Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(workspaceId, userId);
    }
}