package store.lastdance.repository.redis;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import store.lastdance.domain.notification.NotificationRead;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationReadRepository extends CrudRepository<NotificationRead, String> {
    List<NotificationRead> findByUserId(UUID userId);
    boolean existsByIdAndUserId(String notificationId, UUID userId);
}
