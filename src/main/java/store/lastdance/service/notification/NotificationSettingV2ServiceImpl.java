package store.lastdance.service.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import store.lastdance.converter.notification.NotificationSettingConverter;
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
    private final NotificationSettingConverter notificationSettingConverter;

    @Override
    public NotificationSettingResponseDTO getUserSetting(UUID userId) {
        try{
            NotificationSetting setting = settingRepository.findByUserId(userId).orElse(null);
            if (setting == null) createDefaultSetting(userId);
            if (setting == null) throw new CustomException(ErrorCode.NOTIFICATION_SETTING_NOT_FOUND);

            return notificationSettingConverter.toDto(setting);
        } catch (Exception e) {
            throw new CustomException(ErrorCode.NOTIFICATION_SETTING_UPDATE_FAILED);
        }
    }

    @Override
    public void updateSetting(UUID userId, NotificationSettingRequestDTO request) {
        try{
            NotificationSetting setting = settingRepository.findByUserId(userId).orElse(null);

            if (setting == null) throw new CustomException(ErrorCode.NOTIFICATION_SETTING_NOT_FOUND);

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
        } catch (Exception e) {
            throw new CustomException(ErrorCode.NOTIFICATION_SETTING_UPDATE_FAILED);
        }
    }

    @Override
    public void createDefaultSetting(UUID userId) {
        try {
            NotificationSetting existing = settingRepository.findByUserId(userId).orElse(null);
            if (existing != null) throw new CustomException(ErrorCode.NOTIFICATION_SETTING_ALREADY_EXISTS);

            NotificationSetting defaultSetting = notificationSettingConverter.toEntity(userId);
            
            settingRepository.save(defaultSetting);

        } catch (Exception e) {
            throw new CustomException(ErrorCode.NOTIFICATION_SETTING_CREATE_FAILED);
        }
    }
}
