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
        name = "chat_message_sources",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_chat_message_source_rank",
                        columnNames = {
                                "message_id",
                                "source_rank"
                        }
                )
        },
        indexes = {
                @Index(
                        name = "idx_chat_message_sources_message",
                        columnList = "message_id"
                )
        }
)
public class ChatMessageSource {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "message_id",
            nullable = false
    )
    private ChatMessage message;

    @Column(
            name = "source_rank",
            nullable = false
    )
    private Integer sourceRank;

    @Column(
            name = "chunk_id",
            nullable = false
    )
    private UUID chunkId;

    @Column(
            name = "document_id",
            nullable = false
    )
    private UUID documentId;

    @Column(
            name = "document_title",
            nullable = false,
            length = 255
    )
    private String documentTitle;

    @Column(
            name = "chunk_index",
            nullable = false
    )
    private Integer chunkIndex;

    @Column(
            name = "content",
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String content;

    @Column(name = "distance")
    private Double distance;

    @Column(name = "similarity")
    private Double similarity;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}