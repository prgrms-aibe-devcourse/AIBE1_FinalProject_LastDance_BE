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

        try {
            if (sseService.sendNotification(userId, title, content, type, relatedId)) {
                delivered = true;
                log.info("SSE로 실시간 알림 전송 완료: userId={}, type={}", userId, type);
            }
        } catch (Exception e) {
            log.debug("SSE 알림 전송 실패, 웹푸시로 fallback: userId={}, error={}", userId, e.getMessage());
        }

        if (!delivered && webPushService.hasSubscription(userId)) {
            try {
                if (webPushService.sendNotification(userId, title, content, type, relatedId)) {
                    delivered = true;
                    log.info("웹푸시로 실시간 알림 전송 완료: userId={}, type={}", userId, type);
                }
            } catch (Exception e) {
                log.warn("웹푸시 알림 전송 실패: userId={}, error={}", userId, e.getMessage());
            }
        }

        if (!delivered) {
            log.info("모든 실시간 알림 전송 실패 - 이메일 알림으로 대체 필요: userId={}, type={}", userId, type);
        } else {
            log.debug("알림 전송 성공: userId={}, type={}, delivered={}", userId, type, delivered);
        }
    }
}