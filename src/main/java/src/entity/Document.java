package src.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "documents")
public class Document {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by", nullable = false)
    private User uploadedBy;

    private String title;
    private String originalFilename;
    private String fileType;
    private Long fileSize;

    @Column(columnDefinition = "TEXT")
    private String storagePath;

    private String uploadStatus;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}