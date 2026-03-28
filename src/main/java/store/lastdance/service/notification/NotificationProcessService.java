package store.lastdance.service.notification;

import store.lastdance.domain.notification.NotificationSetting;
import store.lastdance.domain.user.User;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationProcessService {
    List<NotificationSetting> findAllActiveSettings();
    void checkAndSendNotifications(User user, NotificationSetting setting);
    void checkScheduleNotifications(User user, NotificationSetting setting, LocalDateTime reminderTime);
    void checkPaymentNotifications(User user, NotificationSetting setting, LocalDateTime now);
    void checkChecklistNotifications(User user, NotificationSetting setting, LocalDateTime now);
    String getMailProviderByUser(User user);
}
