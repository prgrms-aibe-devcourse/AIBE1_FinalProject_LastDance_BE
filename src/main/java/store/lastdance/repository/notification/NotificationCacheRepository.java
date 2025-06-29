package store.lastdance.repository.notification;

import org.springframework.data.repository.CrudRepository;
import store.lastdance.domain.notification.NotificationCache;
import store.lastdance.domain.notification.NotificationType;

import java.util.List;
import java.util.UUID;

public interface NotificationCacheRepository extends CrudRepository<NotificationCache, String> {
    
    /**
     * 사용자별 알림 조회
     */
    List<NotificationCache> findByUserId(UUID userId);
    
    /**
     * 사용자별 특정 타입 알림 조회
     */
    List<NotificationCache> findByUserIdAndType(UUID userId, NotificationType type);
    
    /**
     * 중복 알림 확인
     */
    default boolean isAlreadyNotified(UUID userId, NotificationType type, String relatedId) {
        String key = NotificationCache.generateKey(userId, type, relatedId);
        return existsById(key);
    }
}
