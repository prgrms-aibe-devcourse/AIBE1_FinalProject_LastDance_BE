package store.lastdance.service.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import store.lastdance.domain.notification.NotificationRead;
import store.lastdance.domain.notification.NotificationType;
import store.lastdance.exception.CustomException;
import store.lastdance.exception.ErrorCode;
import store.lastdance.repository.redis.NotificationReadRepository;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationV2ServiceImpl implements NotificationV2Service {

    private final NotificationReadRepository notificationReadRepository;

    @Override
    public void markNotificationAsRead(UUID userId, String notificationId) {
        String[] parts = notificationId.split(":", 3);
        if (parts.length < 3) {
            throw new CustomException(ErrorCode.NOTIFICATION_INVALID_ID_FORMAT);
        }

        if (!userId.toString().equals(parts[0])) {
            log.warn("알림 읽음 처리 권한 없음: requestUserId={}, notificationOwner={}", userId, parts[0]);
            throw new CustomException(ErrorCode.NOTIFICATION_INVALID_ID_FORMAT);
        }

        NotificationType type = NotificationType.valueOf(parts[1]);
        String relatedId = parts[2];

        NotificationRead notificationRead = NotificationRead.create(
            notificationId, userId, type, relatedId
        );
        notificationReadRepository.save(notificationRead);
    }

    @Override
    public boolean isNotificationRead(UUID userId, String notificationId) {
        return notificationReadRepository.existsByIdAndUserId(notificationId, userId);
    }
}