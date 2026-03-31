package store.lastdance.repository.notification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import store.lastdance.domain.notification.NotificationSetting;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationSettingRepository extends JpaRepository<NotificationSetting, Long> {
    Optional<NotificationSetting> findByUserId(UUID userId);

    @Query("SELECT ns FROM NotificationSetting ns JOIN FETCH ns.user u " +
           "WHERE (ns.emailEnabled = true OR ns.sseEnabled = true) " +
           "AND u.isActive = true AND u.isBanned = false")
    List<NotificationSetting> findAllActiveWithUser();
}