package src.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "document_chunks",
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = {"document_id", "chunk_index"}
                )
        }
)
public class DocumentChunk {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @Column(nullable = false)
    private Integer chunkIndex;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    private Integer tokenCount;

    private LocalDateTime createdAt;
}