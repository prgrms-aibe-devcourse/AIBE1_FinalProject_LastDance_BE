package store.lastdance.service.notification;

import java.util.UUID;

public interface NotificationV2Service {
    void markNotificationAsRead(UUID userId, String notificationId);
    boolean isNotificationRead(UUID userId, String notificationId);
}
