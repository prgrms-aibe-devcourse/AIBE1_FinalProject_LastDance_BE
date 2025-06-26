package store.lastdance.domain.notification;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

import java.time.LocalDateTime;
import java.util.UUID;

@RedisHash(value = "notification", timeToLive = 2592000) // 30일 TTL
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class NotificationCache {
    
    @Id
    private String id; // 형식: userId:type:relatedId
    
    @Indexed
    private UUID userId;
    
    @Indexed
    private NotificationType type;
    
    private String title;
    
    private String content;
    
    private String relatedId;
    
    private LocalDateTime sentAt;
    
    /**
     * 캐시 키 생성
     */
    public static String generateKey(UUID userId, NotificationType type, String relatedId) {
        return String.format("%s:%s:%s", userId.toString(), type.name(), relatedId);
    }
    
    /**
     * 정적 팩토리 메서드
     */
    public static NotificationCache create(UUID userId, NotificationType type, String title, 
                                         String content, String relatedId) {
        String key = generateKey(userId, type, relatedId);
        
        return NotificationCache.builder()
                .id(key)
                .userId(userId)
                .type(type)
                .title(title)
                .content(content)
                .relatedId(relatedId)
                .sentAt(LocalDateTime.now())
                .build();
    }
}
