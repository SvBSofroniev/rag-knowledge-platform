package src.workspace.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import src.entity.User;
import src.entity.Workspace;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(
        name = "workspace_messages",
        indexes = {
                @Index(
                        name = "idx_workspace_messages_workspace_created",
                        columnList = "workspace_id, created_at"
                )
        }
)
public class WorkspaceMessage {

    @Id
    @GeneratedValue(
            strategy = GenerationType.UUID
    )
    private UUID id;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "workspace_id",
            nullable = false
    )
    private Workspace workspace;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "user_id",
            nullable = false
    )
    private User user;

    @Column(
            name = "content",
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String content;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt =
                LocalDateTime.now();
    }
}