package store.lastdance.service.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import store.lastdance.domain.notification.NotificationCache;
import store.lastdance.domain.notification.NotificationType;
import store.lastdance.repository.notification.NotificationCacheRepository;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class HybridNotificationService {

    private final SSENotificationService sseService;
    private final WebPushService webPushService;
    private final NotificationCacheRepository cacheRepository;

    /**
     * SSE → 웹푸시 순서로 실시간 알림 전송 (이메일은 스케줄러에서 별도 처리)
     * 캐시 저장은 스케줄러에서 먼저 처리하므로 여기서는 실시간 알림만 담당
     */
    public void sendNotification(UUID userId, NotificationType type, String title, String content, String relatedId) {

        boolean delivered = false;
        String deliveryMethod = "none";

        // 1단계: SSE 시도
        if (sseService.isUserOnline(userId)) {
            if (sseService.sendNotification(userId, title, content, type)) {
                delivered = true;
                deliveryMethod = "sse";
                log.info("SSE로 실시간 알림 전송 완료: userId={}, type={}", userId, type);
            }
        }

        // 2단계: SSE 실패시 웹푸시 시도
        if (!delivered && webPushService.hasSubscription(userId)) {
            if (webPushService.sendNotification(userId, title, content, type)) {
                delivered = true;
                deliveryMethod = "webpush";
                log.info("웹푸시로 실시간 알림 전송 완료: userId={}, type={}", userId, type);
            }
        }

        // 실시간 알림 실패 시 로그만 남김 (이메일은 스케줄러에서 처리)
        if (!delivered) {
            log.info("실시간 알림 전송 실패 - 이메일로 대체됨: userId={}, type={}", userId, type);
        }
    }
}