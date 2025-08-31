package store.lastdance.service.notification;

import store.lastdance.domain.user.User;
import store.lastdance.dto.notification.NotificationSettingRequestDTO;
import store.lastdance.dto.notification.NotificationSettingResponseDTO;

import java.util.List;
import java.util.UUID;

public interface NotificationSettingV2Service {
    NotificationSettingResponseDTO getUserSetting(UUID userId);
    void updateSetting(UUID userId, NotificationSettingRequestDTO request);
    
    void createDefaultSetting(UUID userId);
}