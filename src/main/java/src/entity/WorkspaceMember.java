package src.entity;

import jakarta.persistence.*;
import src.entity.embeddable.WorkspaceMemberId;

import java.time.LocalDateTime;

@Entity
@Table(name = "workspace_members")
public class WorkspaceMember {

    @EmbeddedId
    private WorkspaceMemberId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("workspaceId")
    @JoinColumn(name = "workspace_id")
    private Workspace workspace;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    private String role;

    private LocalDateTime joinedAt;
}
