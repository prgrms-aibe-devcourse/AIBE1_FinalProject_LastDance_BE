package store.lastdance.service.notification;

import store.lastdance.domain.user.User;
import store.lastdance.dto.notification.NotificationSettingRequestDTO;
import store.lastdance.dto.notification.NotificationSettingResponseDTO;

import java.util.List;
import java.util.UUID;

public interface NotificationSettingService {
    NotificationSettingResponseDTO getUserSetting(UUID userId);
    void updateSetting(UUID userId, NotificationSettingRequestDTO request);
    
    // 새 사용자의 기본 알림 설정 생성
    void createDefaultSetting(UUID userId);

    List<User> emailPermitted();
    List<User> ssePermitted();
    List<User> webPushPermitted();
    
    // 통합 메서드 (NotificationType 기반)
    boolean getSSEEnabledUserForNotificationType(UUID userId, store.lastdance.domain.notification.NotificationType type);
    boolean getWebPushEnabledUserForNotificationType(UUID userId, store.lastdance.domain.notification.NotificationType type);
}
