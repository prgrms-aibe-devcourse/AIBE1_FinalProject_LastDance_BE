package store.lastdance.domain.ai;

import lombok.*;
import jakarta.persistence.*;
import store.lastdance.domain.group.Group;
import store.lastdance.domain.user.User;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ai_judgments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiJudgment {
    @Id
    @Column(name = "judgment_id")
    private UUID judgmentId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "group_id")
    private UUID groupId;

    @Column(name = "type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private JudgmentType type;

    @Column(name = "situation", nullable = false, columnDefinition = "TEXT")
    private String situation;

    @Column(name = "judgment_result", nullable = false, columnDefinition = "TEXT")
    private String judgmentResult;

    @Column(name = "up")
    private Boolean up;

    @Column(name = "down")
    private Boolean down;

    @Column(name = "down_reason", columnDefinition = "TEXT")
    private String downReason;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", insertable = false, updatable = false)
    private Group group;

    @Builder
    public AiJudgment(@NonNull UUID judgmentId, @NonNull UUID userId, @NonNull JudgmentType type,
                      @NonNull String situation, @NonNull String judgmentResult) {
        this.judgmentId = judgmentId;
        this.userId = userId;
        this.type = type;
        this.situation = situation;
        this.judgmentResult = judgmentResult;
    }

    public void setGroupId(UUID groupId) {
        this.groupId = groupId;
    }

    public void feedback(Boolean up, Boolean down, String downReason) {
        this.up = up;
        this.down = down;
        this.downReason = downReason;
    }
}
