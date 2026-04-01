package store.lastdance.service.notification;

import store.lastdance.domain.notification.NotificationType;

public interface MailV2Service {

    void sendNotification(String to, NotificationType type, String title, String content, String provider);
}
