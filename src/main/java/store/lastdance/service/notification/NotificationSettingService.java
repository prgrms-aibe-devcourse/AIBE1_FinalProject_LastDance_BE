package store.lastdance.service.notification;

import store.lastdance.dto.notification.NotificationSettingRequestDTO;
import store.lastdance.dto.notification.NotificationSettingResponseDTO;

import java.util.UUID;

public interface NotificationSettingService {
    NotificationSettingResponseDTO getUserSetting(UUID userId);
    void updateSetting(UUID userId, NotificationSettingRequestDTO request);
}
