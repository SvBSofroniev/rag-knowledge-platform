package src.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import src.workspace.util.WorkspaceRole;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(
        name = "workspace_members",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_workspace_member",
                        columnNames = {"workspace_id", "user_id"}
                )
        },
        indexes = {
                @Index(
                        name = "idx_workspace_members_user",
                        columnList = "user_id"
                )
        }
)
public class WorkspaceMember {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "role",
            nullable = false,
            length = 20
    )
    private WorkspaceRole role = WorkspaceRole.MEMBER;

    @Column(
            name = "joined_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime joinedAt;

    @PrePersist
    protected void onCreate() {
        joinedAt = LocalDateTime.now();

        if (role == null) {
            role = WorkspaceRole.MEMBER;
        }
    }
}