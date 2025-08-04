package store.lastdance.repository.redis;

import org.springframework.data.repository.CrudRepository;
import store.lastdance.domain.notification.NotificationCache;
import store.lastdance.domain.notification.NotificationType;

import java.util.List;
import java.util.UUID;

public interface NotificationCacheRepository extends CrudRepository<NotificationCache, String> {
    List<NotificationCache> findByUserId(UUID userId);
}
