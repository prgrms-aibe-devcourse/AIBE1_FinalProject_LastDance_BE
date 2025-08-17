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
    private final NotificationSettingService notificationSettingService;

    public void sendNotification(UUID userId, NotificationType type, String title, String content, String relatedId) {
        // 1. SSE 알림 시도 (SSE 설정이 활성화된 경우)
        try {
            if (notificationSettingService.getSSEEnabledUserForNotificationType(userId, type)) {
                if (sseService.sendNotification(userId, title, content, type, relatedId)) {
                    log.info("SSE로 실시간 알림 전송 완료: userId={}, type={}", userId, type);
                }
            } else {
                log.debug("SSE 알림이 비활성화됨: userId={}, type={}", userId, type);
            }
        } catch (Exception e) {
            log.debug("SSE 알림 전송 실패: userId={}, error={}", userId, e.getMessage());
        }
    }
}