package src.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import src.document.util.DocumentStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(
        name = "documents",
        indexes = {
                @Index(
                        name = "idx_documents_workspace",
                        columnList = "workspace_id"
                ),
                @Index(
                        name = "idx_documents_uploaded_by",
                        columnList = "uploaded_by"
                ),
                @Index(
                        name = "idx_documents_status",
                        columnList = "upload_status"
                )
        }
)
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploaded_by", nullable = false)
    private User uploadedBy;

    @Column(
            name = "title",
            nullable = false,
            length = 255
    )
    private String title;

    @Column(
            name = "original_filename",
            nullable = false,
            length = 255
    )
    private String originalFilename;

    @Column(
            name = "file_type",
            nullable = false,
            length = 100
    )
    private String fileType;

    @Column(
            name = "file_size",
            nullable = false
    )
    private Long fileSize;

    @Column(
            name = "storage_path",
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String storagePath;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "upload_status",
            nullable = false,
            length = 20
    )
    private DocumentStatus status = DocumentStatus.PENDING;

    @Column(
            name = "processing_error",
            columnDefinition = "TEXT"
    )
    private String processingError;

    @OneToMany(
            mappedBy = "document",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<DocumentChunk> chunks = new ArrayList<>();

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    @Column(
            name = "updated_at",
            nullable = false
    )
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();

        createdAt = now;
        updatedAt = now;

        if (status == null) {
            status = DocumentStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}