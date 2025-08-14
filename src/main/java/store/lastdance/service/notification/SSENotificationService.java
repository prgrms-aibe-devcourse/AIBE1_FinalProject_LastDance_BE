package store.lastdance.service.notification;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import store.lastdance.domain.notification.NotificationType;

import java.util.UUID;

public interface SSENotificationService {
    SseEmitter createConnection(UUID userId);
    void disconnectUser(UUID userId);
    boolean sendNotification(UUID userId, String title, String content, NotificationType type, String relatedId);
    boolean isUserOnline(UUID userId);
    void cleanupInactiveConnections();
    int getActiveConnectionCount();
    void shutdown();
}
