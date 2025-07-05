package store.lastdance.service.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import store.lastdance.domain.notification.NotificationType;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class HybridNotificationService {

    private final SSENotificationService sseService;
    private final WebPushService webPushService;

    public void sendNotification(UUID userId, NotificationType type, String title, String content, String relatedId) {
        boolean delivered = false;

        if (sseService.isUserOnline(userId)) {
            if (sseService.sendNotification(userId, title, content, type, relatedId)) {
                delivered = true;
                log.info("SSE로 실시간 알림 전송 완료: userId={}, type={}", userId, type);
            }
        }

        if (!delivered && webPushService.hasSubscription(userId)) {
            if (webPushService.sendNotification(userId, title, content, type, relatedId)) {
                delivered = true;
                log.info("웹푸시로 실시간 알림 전송 완료: userId={}, type={}", userId, type);
            }
        }

        if (!delivered) {
            log.info("실시간 알림 전송 실패: userId={}, type={}", userId, type);
        }
    }
}