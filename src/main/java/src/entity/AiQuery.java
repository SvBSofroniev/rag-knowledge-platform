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
        name = "ai_queries",
        indexes = {
                @Index(
                        name = "idx_ai_queries_user",
                        columnList = "user_id"
                ),
                @Index(
                        name = "idx_ai_queries_session",
                        columnList = "chat_session_id"
                )
        }
)
public class AiQuery {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_session_id")
    private ChatSession chatSession;

    @Column(
            name = "query_text",
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String queryText;

    @Column(
            name = "response_text",
            columnDefinition = "TEXT"
    )
    private String responseText;

    @Column(name = "model_name", length = 100)
    private String modelName;

    @Column(name = "response_time_ms")
    private Integer responseTimeMs;

    @Column(name = "prompt_tokens")
    private Integer promptTokens;

    @Column(name = "completion_tokens")
    private Integer completionTokens;

    @Column(name = "total_tokens")
    private Integer totalTokens;

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