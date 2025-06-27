package store.lastdance.service.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import store.lastdance.domain.notification.NotificationSetting;
import store.lastdance.dto.notification.NotificationSettingRequestDTO;
import store.lastdance.dto.notification.NotificationSettingResponseDTO;
import store.lastdance.repository.notification.NotificationSettingRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationSettingServiceImpl implements NotificationSettingService {

    private final NotificationSettingRepository settingRepository;

    @Override
    public NotificationSettingResponseDTO getUserSetting(UUID userId) {
        NotificationSetting setting = settingRepository.findByUserId(userId);
        if (setting == null) {
            return NotificationSettingResponseDTO.builder()
                    .settingId(null)
                    .userId(userId)
                    .emailEnabled(false)
                    .scheduleReminder(false)
                    .paymentReminder(false)
                    .checklistReminder(false)
                    .createdAt(null)
                    .build();
        }

        return NotificationSettingResponseDTO.builder()
                .settingId(setting.getSettingId())
                .userId(setting.getUserId())
                .emailEnabled(setting.getEmailEnabled())
                .scheduleReminder(setting.getScheduleReminder())
                .paymentReminder(setting.getPaymentReminder())
                .checklistReminder(setting.getChecklistReminder())
                .createdAt(setting.getCreatedAt())
                .build();
    }

    @Override
    public void updateSetting(UUID userId, NotificationSettingRequestDTO request) {
        NotificationSetting setting = settingRepository.findByUserId(userId);

        if (setting == null) {
            setting = NotificationSetting.builder()
                    .userId(userId)
                    .build();
        }

        setting.updateEmailEnabled(request.getEmailEnabled());
        setting.updateScheduleReminder(request.getScheduleReminder());
        setting.updatePaymentReminder(request.getPaymentReminder());
        setting.updateChecklistReminder(request.getChecklistReminder());

        settingRepository.save(setting);
    }
}
