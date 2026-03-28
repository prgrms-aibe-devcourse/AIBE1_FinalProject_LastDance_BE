package store.lastdance.service.notification;

import store.lastdance.dto.notification.NotificationSettingRequestDTO;
import store.lastdance.dto.notification.NotificationSettingResponseDTO;

import java.util.UUID;

public interface NotificationSettingV2Service {
    NotificationSettingResponseDTO getUserSetting(UUID userId);
    NotificationSettingResponseDTO updateSetting(UUID userId, NotificationSettingRequestDTO request);
    void createDefaultSetting(UUID userId);
}