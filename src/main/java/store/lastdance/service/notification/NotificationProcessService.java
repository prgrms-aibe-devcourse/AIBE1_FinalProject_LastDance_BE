package store.lastdance.service.notification;

import store.lastdance.domain.notification.NotificationSetting;
import store.lastdance.domain.user.User;

import java.util.List;

public interface NotificationProcessService {

    List<NotificationSetting> findAllActiveSettings();

    void checkAndSendNotifications(User user, NotificationSetting setting);
}
