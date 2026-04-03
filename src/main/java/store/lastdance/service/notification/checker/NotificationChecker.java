package store.lastdance.service.notification.checker;

import store.lastdance.domain.notification.NotificationSetting;
import store.lastdance.domain.notification.NotificationType;
import store.lastdance.domain.user.User;

import java.time.LocalDateTime;

public interface NotificationChecker {

    NotificationType getType();

    void check(User user, NotificationSetting setting, LocalDateTime now);
}
