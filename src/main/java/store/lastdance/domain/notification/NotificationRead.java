package store.lastdance.domain.notification;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

import java.time.LocalDateTime;
import java.util.UUID;

@RedisHash(value = "notification_read", timeToLive = 2592000) // 30일 TTL
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class NotificationRead {
    
    @Id
    private String id; // notificationId
    
    @Indexed
    private UUID userId;
    
    @Indexed
    private NotificationType type;
    
    private String relatedId;
    
    private LocalDateTime readAt;
    
    private LocalDateTime createdAt;
    
    /**
     * 정적 팩토리 메서드
     */
    public static NotificationRead create(String notificationId, UUID userId, 
                                        NotificationType type, String relatedId) {
        return NotificationRead.builder()
                .id(notificationId)
                .userId(userId)
                .type(type)
                .relatedId(relatedId)
                .readAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();
    }
}
