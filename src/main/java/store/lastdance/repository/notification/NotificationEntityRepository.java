package store.lastdance.repository.notification;

import org.springframework.data.jpa.repository.JpaRepository;
import store.lastdance.domain.notification.NotificationEntity;
import store.lastdance.domain.notification.NotificationType;

import java.util.List;
import java.util.UUID;

/**
 * 기존 PostgreSQL 기반 Notification Repository (백업용)
 * 현재는 사용하지 않음 - Redis 기반 NotificationCacheRepository 사용
 */
public interface NotificationEntityRepository extends JpaRepository<NotificationEntity, Long> {

    // 특정 사용자의 알림 목록
    List<NotificationEntity> findAllByUserIdOrderBySentAtDesc(UUID userId);

    // 특정 사용자에게 특정 시간 범위 안에 같은 제목의 알림이 이미 전송되었는지 확인 (중복 방지용)
    boolean existsByUserIdAndTitleAndSentAtBetween(UUID userId, String title,
                                                   java.time.LocalDateTime from,
                                                   java.time.LocalDateTime to);

    // 특정 사용자에게 특정 타입과 관련 ID로 알림이 이미 전송되었는지 확인
    boolean existsByUserIdAndTypeAndRelatedId(UUID userId, NotificationType type, String relatedId);
}
