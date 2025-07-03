package store.lastdance.repository.notification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import store.lastdance.domain.notification.NotificationSetting;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationSettingRepository extends JpaRepository<NotificationSetting, Long> {
    Optional<NotificationSetting> findByUserId(UUID userId);
    
    @Query("SELECT ns.userId FROM NotificationSetting ns WHERE ns.emailEnabled = true")
    List<UUID> findUserIdsByEmailEnabledTrue();
}