package store.lastdance.repository.notification;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import store.lastdance.domain.notification.NotificationRead;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationReadRepository extends CrudRepository<NotificationRead, String> {
    
    /**
     * 사용자별 읽은 알림 목록 조회
     */
    List<NotificationRead> findByUserId(UUID userId);
    
    /**
     * 특정 알림이 읽혔는지 확인
     */
    boolean existsByIdAndUserId(String notificationId, UUID userId);
}
