package store.lastdance.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import store.lastdance.domain.notification.NotificationSetting;
import store.lastdance.domain.user.User;
import store.lastdance.service.notification.*;
import store.lastdance.service.notification.sse.SSENotificationV2Service;

import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationScheduler {

    private final NotificationProcessService notificationProcessService;
    private final SSENotificationV2Service sseService;

    @Scheduled(fixedRate = 60000)
    public void processScheduledNotifications() {
        List<NotificationSetting> settings = notificationProcessService.findAllActiveSettings();

        if (settings.isEmpty()) return;

        for (NotificationSetting setting : settings) {
            User user = setting.getUser();
            if (user == null) continue;
            try {
                notificationProcessService.checkAndSendNotifications(user, setting);
            } catch (Exception e) {
                log.error("알림 처리 실패: userId={}, error={}", user.getUserId(), e.getMessage());
            }
        }
    }

    @Scheduled(fixedRate = 300000)
    public void cleanupSSEConnections() {
        try {
            sseService.cleanupInactiveConnections();
        } catch (Exception e) {
            log.error("SSE 연결 정리 실패: {}", e.getMessage());
        }
    }
}