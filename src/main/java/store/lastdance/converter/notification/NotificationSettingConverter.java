package store.lastdance.converter.notification;

import org.springframework.stereotype.Component;
import store.lastdance.domain.notification.NotificationSetting;
import store.lastdance.dto.notification.NotificationSettingResponseDTO;

import java.util.UUID;

@Component
public class NotificationSettingConverter {
    public NotificationSetting toEntity(UUID userId) {
        return NotificationSetting.builder()
                .userId(userId)
                .build();
    }

    public NotificationSettingResponseDTO toDto(NotificationSetting notificationSetting) {
        return NotificationSettingResponseDTO.builder()
                .settingId(notificationSetting.getSettingId())
                .userId(notificationSetting.getUserId())
                .emailEnabled(notificationSetting.isEmailEnabled())
                .scheduleReminder(notificationSetting.isScheduleReminder())
                .paymentReminder(notificationSetting.isPaymentReminder())
                .checklistReminder(notificationSetting.isChecklistReminder())
                .sseEnabled(notificationSetting.isSseEnabled())
                .createdAt(notificationSetting.getCreatedAt())
                .build();
    }
}
