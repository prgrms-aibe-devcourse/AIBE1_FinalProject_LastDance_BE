package store.lastdance.service.notification;

import store.lastdance.domain.notification.NotificationType;

import java.util.UUID;

public interface NotificationService {
    /**
     * 알림 읽음 처리
     */
    void markNotificationAsRead(UUID userId, String notificationId);
    
    /**
     * 알림 읽음 상태 확인
     */
    boolean isNotificationRead(UUID userId, String notificationId);
}
