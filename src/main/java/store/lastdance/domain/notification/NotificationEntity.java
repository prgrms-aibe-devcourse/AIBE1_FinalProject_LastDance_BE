package store.lastdance.domain.notification;

import lombok.*;
import jakarta.persistence.*;
import store.lastdance.domain.user.User;
import store.lastdance.domain.common.BaseTimeEntity;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 기존 PostgreSQL 기반 Notification 엔티티 (백업용)
 * 현재는 사용하지 않음 - Redis 기반 NotificationCache 사용
 */
@Getter
@Entity
@Table(name = "notifications")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationEntity extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long notificationId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private NotificationType type;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "related_id", length = 100)
    private String relatedId;

    @Column(name = "is_sent", nullable = false)
    private Boolean isSent = false;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @Builder
    public NotificationEntity(@NonNull UUID userId, @NonNull NotificationType type, @NonNull String title, @NonNull String content, String relatedId) {
        this.userId = userId;
        this.type = type;
        this.title = title;
        this.content = content;
        this.relatedId = relatedId;
        this.isSent = false;
    }

    public void markAsSent() {
        this.isSent = true;
        this.sentAt = LocalDateTime.now();
    }
}
