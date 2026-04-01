package store.lastdance.service.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import store.lastdance.domain.notification.NotificationCache;
import store.lastdance.domain.notification.NotificationSetting;
import store.lastdance.domain.notification.NotificationType;
import store.lastdance.domain.user.OAuthProvider;
import store.lastdance.domain.user.User;
import store.lastdance.repository.redis.NotificationCacheRepository;

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
            sendMail(user, setting, type, title, content);
            notificationCacheRepository.save(
                    NotificationCache.create(user.getUserId(), type, title, content, relatedId));
        } catch (Exception e) {
            log.error("알림 처리 실패: userId={}, type={}, relatedId={}, error={}",
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
        String provider = user.getProvider() == OAuthProvider.NAVER ? "naver" : "gmail";
        mailService.sendNotification(user.getEmail(), type, title, content, provider);
        log.info("이메일 알림 전송 완료: userId={}, type={}", user.getUserId(), type);
    }
}
