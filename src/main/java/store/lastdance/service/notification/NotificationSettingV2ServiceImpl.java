package store.lastdance.service.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import store.lastdance.domain.notification.NotificationSetting;
import store.lastdance.dto.notification.NotificationSettingRequestDTO;
import store.lastdance.dto.notification.NotificationSettingResponseDTO;
import store.lastdance.exception.CustomException;
import store.lastdance.exception.ErrorCode;
import store.lastdance.repository.notification.NotificationSettingRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationSettingV2ServiceImpl implements NotificationSettingV2Service {

    private final NotificationSettingRepository settingRepository;

    @Override
    public NotificationSettingResponseDTO getUserSetting(UUID userId) {
        NotificationSetting setting = settingRepository.findByUserId(userId).orElse(null);
        if (setting == null) {
            createDefaultSetting(userId);
            return NotificationSettingResponseDTO.builder()
                    .settingId(null)
                    .userId(userId)
                    .emailEnabled(false)
                    .scheduleReminder(false)
                    .paymentReminder(false)
                    .checklistReminder(false)
                    .sseEnabled(false)
                    .createdAt(null)
                    .build();
        }

        return NotificationSettingResponseDTO.builder()
                .settingId(setting.getSettingId())
                .userId(setting.getUserId())
                .emailEnabled(setting.getEmailEnabled() != null ? setting.getEmailEnabled() : true)
                .scheduleReminder(setting.getScheduleReminder() != null ? setting.getScheduleReminder() : true)
                .paymentReminder(setting.getPaymentReminder() != null ? setting.getPaymentReminder() : true)
                .checklistReminder(setting.getChecklistReminder() != null ? setting.getChecklistReminder() : true)
                .sseEnabled(setting.getSseEnabled() != null ? setting.getSseEnabled() : true)
                .createdAt(setting.getCreatedAt())
                .build();
    }

    @Override
    public void updateSetting(UUID userId, NotificationSettingRequestDTO request) {
        NotificationSetting setting = settingRepository.findByUserId(userId).orElse(null);

        if (setting == null) {
            setting = NotificationSetting.builder()
                    .userId(userId)
                    .build();
        }

        if (request.getEmailEnabled() != null) {
            setting.updateEmailEnabled(request.getEmailEnabled());
        }
        if (request.getScheduleReminder() != null) {
            setting.updateScheduleReminder(request.getScheduleReminder());
        }
        if (request.getPaymentReminder() != null) {
            setting.updatePaymentReminder(request.getPaymentReminder());
        }
        if (request.getChecklistReminder() != null) {
            setting.updateChecklistReminder(request.getChecklistReminder());
        }
        if (request.getSseEnabled() != null) {
            setting.updateSSEEnabled(request.getSseEnabled());
        }
        settingRepository.save(setting);
    }

    @Override
    public void createDefaultSetting(UUID userId) {
        try {
            NotificationSetting existing = settingRepository.findByUserId(userId).orElse(null);
            if (existing != null) throw new CustomException(ErrorCode.NOTIFICATION_SETTING_ALREADY_EXISTS);

            NotificationSetting defaultSetting = NotificationSetting.builder()
                    .userId(userId)
                    .build();
            
            settingRepository.save(defaultSetting);

        } catch (Exception e) {
            throw new CustomException(ErrorCode.NOTIFICATION_SETTING_CREATE_FAILED);
        }
    }
}
