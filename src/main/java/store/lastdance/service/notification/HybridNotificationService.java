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
     */
    public void sendNotification(UUID userId, NotificationType type, String title, String content, String relatedId) {

        // 중복 발송 방지 체크
        String cacheKey = NotificationCache.generateKey(userId, type, relatedId);
        if (cacheRepository.existsById(cacheKey)) {
            log.debug("이미 발송된 알림: userId={}, type={}, relatedId={}", userId, type, relatedId);
            return;
        }

        boolean delivered = false;
        String deliveryMethod = "none";

        // 1단계: SSE 시도
        if (sseService.isUserOnline(userId)) {
            if (sseService.sendNotification(userId, title, content, type)) {
                delivered = true;
                deliveryMethod = "sse";
                log.info("SSE로 알림 전송 완료: userId={}, type={}", userId, type);
            }
        }

        // 2단계: SSE 실패시 웹푸시 시도
        if (!delivered && webPushService.hasSubscription(userId)) {
            if (webPushService.sendNotification(userId, title, content, type)) {
                delivered = true;
                deliveryMethod = "webpush";
                log.info("웹푸시로 알림 전송 완료: userId={}, type={}", userId, type);
            }
        }

        // 전송 기록 저장 (실시간 알림 성공한 경우만)
        if (delivered) {
            NotificationCache cache = NotificationCache.create(userId, type, title,
                    "알림이 " + deliveryMethod + "로 전송됨", relatedId);
            cacheRepository.save(cache);
        }
    }
}