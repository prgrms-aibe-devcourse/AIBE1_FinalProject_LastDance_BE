package store.lastdance.service.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import store.lastdance.domain.notification.NotificationCache;
import store.lastdance.domain.notification.NotificationSetting;
import store.lastdance.domain.notification.NotificationType;
import store.lastdance.domain.user.User;
import store.lastdance.repository.redis.NotificationCacheRepository;
import store.lastdance.service.notification.mail.MailV2Service;
import store.lastdance.service.notification.sse.SSENotificationV2Service;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationSender {

    private final NotificationCacheRepository notificationCacheRepository;
    private final SSENotificationV2Service sseService;
    private final MailV2Service mailService;

    public void sendIfNotCached(User user, NotificationSetting setting,
                                NotificationType type, String title, String content, String relatedId) {
        String cacheKey = NotificationCache.generateKey(user.getUserId(), type, relatedId);
        if (notificationCacheRepository.existsById(cacheKey)) return;

        try {
            sendSSE(user, setting, type, title, content, relatedId);
        } catch (Exception e) {
            log.error("SSE 알림 실패: userId={}, type={}, error={}", user.getUserId(), type, e.getMessage());
        }

        try {
            sendMail(user, setting, type, title, content);
        } catch (Exception e) {
            log.error("메일 알림 실패: userId={}, type={}, error={}", user.getUserId(), type, e.getMessage());
        }

        try {
            notificationCacheRepository.save(
                    NotificationCache.create(user.getUserId(), type, title, content, relatedId));
        } catch (Exception e) {
            log.error("알림 캐시 저장 실패: userId={}, type={}, relatedId={}, error={}",
                    user.getUserId(), type, relatedId, e.getMessage());
        }
    }

    private void sendSSE(User user, NotificationSetting setting,
                         NotificationType type, String title, String content, String relatedId) {
        if (!setting.isSseEnabled()) return;
        if (sseService.sendNotification(user.getUserId(), title, content, type, relatedId)) {
            log.info("SSE 알림 전송 완료: userId={}, type={}", user.getUserId(), type);
        }
    }

    private void sendMail(User user, NotificationSetting setting,
                          NotificationType type, String title, String content) {
        if (!setting.isEmailEnabledForType(type)) return;
        mailService.sendNotification(user.getEmail(), type, title, content, user.getProvider());
        log.info("이메일 알림 전송 완료: userId={}, type={}", user.getUserId(), type);
    }
}
