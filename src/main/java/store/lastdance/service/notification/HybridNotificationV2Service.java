package store.lastdance.service.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import store.lastdance.domain.notification.NotificationType;
import store.lastdance.exception.CustomException;
import store.lastdance.exception.ErrorCode;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class HybridNotificationV2Service {

    private final SSENotificationV2Service sseService;
    private final NotificationSettingV2Service notificationSettingService;

    public void sendNotification(UUID userId, NotificationType type, String title, String content, String relatedId) {
        try {
            if (notificationSettingService.getSSEEnabledUserForNotificationType(userId, type)) {
                if (sseService.sendNotification(userId, title, content, type, relatedId)) {
                    log.info("SSE로 실시간 알림 전송 완료: userId={}, type={}", userId, type);
                }
            }
        } catch (CustomException e) {
            throw new CustomException(ErrorCode.NOTIFICATION_SSE_SEND_FAILED);
        }
    }
}