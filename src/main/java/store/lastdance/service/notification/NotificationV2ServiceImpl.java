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
        try {
            String[] parts = notificationId.split(":");
            if (parts.length >= 3) {
                NotificationType type = NotificationType.valueOf(parts[1]);
                String relatedId = parts[2];
                
                NotificationRead notificationRead = NotificationRead.create(
                    notificationId, userId, type, relatedId
                );
                notificationReadRepository.save(notificationRead);
                
            } else {
                log.warn("잘못된 알림 ID 형식 - notificationId: {}", notificationId);
            }
        } catch (CustomException e) {
            throw new CustomException(ErrorCode.NOTIFICATION_READ_FAILED);
        }
    }
    
    @Override
    public boolean isNotificationRead(UUID userId, String notificationId) {
        return notificationReadRepository.existsByIdAndUserId(notificationId, userId);
    }
}
