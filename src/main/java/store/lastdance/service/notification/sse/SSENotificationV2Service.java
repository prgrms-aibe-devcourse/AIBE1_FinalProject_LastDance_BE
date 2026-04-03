package store.lastdance.service.notification.sse;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import store.lastdance.domain.notification.NotificationType;

import java.util.UUID;

public interface SSENotificationV2Service {
    SseEmitter createConnection(UUID userId, String connectionId);
    void disconnectUser(UUID userId);
    void disconnectConnection(UUID userId, String connectionId);
    boolean sendNotification(UUID userId, String title, String content, NotificationType type, String relatedId);
    void cleanupInactiveConnections();
    void shutdown();
}
