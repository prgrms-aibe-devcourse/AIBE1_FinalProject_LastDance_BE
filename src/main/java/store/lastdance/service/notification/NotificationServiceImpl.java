package store.lastdance.service.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import store.lastdance.domain.notification.NotificationRead;
import store.lastdance.domain.notification.NotificationType;
import store.lastdance.repository.notification.NotificationReadRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
    
    private final HybridNotificationService hybridNotificationService;
    private final NotificationReadRepository notificationReadRepository;
    
    @Override
    public void sendTestNotification(UUID userId, NotificationType type, String title, String content, String relatedId) {

        // 하이브리드 알림 시스템으로 전송
        hybridNotificationService.sendNotification(userId, type, title, content, relatedId);
        
    }
    
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
                
            }
        } catch (Exception e) {
        }
    }
    
    @Override
    public boolean isNotificationRead(UUID userId, String notificationId) {
        return notificationReadRepository.existsByIdAndUserId(notificationId, userId);
    }
}
