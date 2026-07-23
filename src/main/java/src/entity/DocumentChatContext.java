package src.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(
        name = "document_chat_context",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_chat_session_document",
                        columnNames = {
                                "chat_session_id",
                                "document_id"
                        }
                )
        },
        indexes = {
                @Index(
                        name = "idx_document_chat_context_document",
                        columnList = "document_id"
                )
        }
)
public class DocumentChatContext {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "chat_session_id",
            nullable = false
    )
    private ChatSession chatSession;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "document_id",
            nullable = false
    )
    private Document document;

    @Column(
            name = "attached_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime attachedAt;

    @PrePersist
    protected void onCreate() {
        attachedAt = LocalDateTime.now();
    }
}