package store.lastdance.repository.notification;

import org.springframework.data.jpa.repository.JpaRepository;
import store.lastdance.domain.notification.NotificationSetting;

import java.util.UUID;

public interface NotificationSettingRepository extends JpaRepository<NotificationSetting, Long> {
    NotificationSetting findByUserId(UUID userId);
}