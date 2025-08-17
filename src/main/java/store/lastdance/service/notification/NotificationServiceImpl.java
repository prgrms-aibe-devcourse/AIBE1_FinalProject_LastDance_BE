package store.lastdance.service.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import store.lastdance.domain.notification.NotificationRead;
import store.lastdance.domain.notification.NotificationType;
import store.lastdance.repository.redis.NotificationReadRepository;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
    
    private final HybridNotificationService hybridNotificationService;
    private final NotificationReadRepository notificationReadRepository;
    
    @Override
    public void markNotificationAsRead(UUID userId, String notificationId) {
        try {
            // 알림 ID 파싱하여 타입과 relatedId 추출
            String[] parts = notificationId.split(":");
            if (parts.length >= 3) {
                NotificationType type = NotificationType.valueOf(parts[1]);
                String relatedId = parts[2];
                
                // 읽음 상태 저장
                NotificationRead notificationRead = NotificationRead.create(
                    notificationId, userId, type, relatedId
                );
                notificationReadRepository.save(notificationRead);
                
                log.info("알림 읽음 처리 완료 - userId: {}, notificationId: {}", userId, notificationId);
            } else {
                log.warn("잘못된 알림 ID 형식 - notificationId: {}", notificationId);
            }
        } catch (Exception e) {
            log.error("알림 읽음 처리 실패 - userId: {}, notificationId: {}, error: {}", 
                     userId, notificationId, e.getMessage());
        }
    }
    
    @Override
    public boolean isNotificationRead(UUID userId, String notificationId) {
        return notificationReadRepository.existsByIdAndUserId(notificationId, userId);
    }
}
