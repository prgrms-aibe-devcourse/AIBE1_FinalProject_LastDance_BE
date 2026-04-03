package store.lastdance.service.notification.mail;

import store.lastdance.domain.notification.NotificationType;
import store.lastdance.domain.user.OAuthProvider;

public interface MailV2Service {

    void sendNotification(String to, NotificationType type, String title, String content, OAuthProvider provider);
}
