package store.lastdance.service.notification;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import store.lastdance.domain.notification.NotificationType;

import java.util.UUID;

public interface SSENotificationV2Service {
    SseEmitter createConnection(UUID userId);
    void disconnectUser(UUID userId);
    boolean sendNotification(UUID userId, String title, String content, NotificationType type, String relatedId);
    void cleanupInactiveConnections();
    void shutdown();
}
