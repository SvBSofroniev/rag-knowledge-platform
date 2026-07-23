package src.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Array;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(
        name = "document_chunks",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_document_chunk",
                        columnNames = {"document_id", "chunk_index"}
                )
        }
)
public class DocumentChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

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

    @Column(name = "token_count")
    private Integer tokenCount;

    @JdbcTypeCode(SqlTypes.VECTOR)
    @Array(length = 768)
    @Column(
            name = "embedding",
            nullable = false,
            columnDefinition = "vector(768)"
    )
    private float[] embedding;

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