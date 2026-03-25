package store.lastdance.service.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import store.lastdance.domain.notification.NotificationSetting;
import store.lastdance.domain.notification.NotificationType;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class HybridNotificationV2Service {

    private final SSENotificationV2Service sseService;

    public void sendNotification(UUID userId, NotificationType type, String title, String content, String relatedId, NotificationSetting setting) {
        if (setting.isSseEnabled() && setting.isNotificationEnabled(type)) {
            sendSSE(userId, type, title, content, relatedId);
        }
    }

    private void sendSSE(UUID userId, NotificationType type, String title, String content, String relatedId) {
        if (sseService.sendNotification(userId, title, content, type, relatedId)) {
            log.info("SSE로 실시간 알림 전송 완료: userId={}, type={}", userId, type);
        }
    }
}
