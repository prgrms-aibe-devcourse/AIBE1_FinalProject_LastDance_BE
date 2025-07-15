package store.lastdance.service.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import store.lastdance.domain.notification.NotificationType;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HybridNotificationService {

    private final SSENotificationService sseService;
    private final WebPushService webPushService;
    private final NotificationSettingService notificationSettingService;

    public void sendNotification(UUID userId, NotificationType type, String title, String content, String relatedId) {
        boolean delivered = false;

        // 1. SSE 알림 시도 (SSE 설정이 활성화된 경우)
        try {
            if (notificationSettingService.getSSEEnabledUserForNotificationType(userId, type)) {
                if (sseService.sendNotification(userId, title, content, type, relatedId)) {
                    delivered = true;
                }
            }
        } catch (Exception e) {
        }

        // 2. 웹푸시 fallback (SSE 실패하거나 비활성화된 경우)
        if (!delivered) {
            try {
                if (notificationSettingService.getWebPushEnabledUserForNotificationType(userId, type)) {
                    if (webPushService.hasSubscription(userId)) {
                        if (webPushService.sendNotification(userId, title, content, type, relatedId)) {
                            delivered = true;
                        }
                    }
                }
            } catch (Exception e) {
            }
        }



    }
}