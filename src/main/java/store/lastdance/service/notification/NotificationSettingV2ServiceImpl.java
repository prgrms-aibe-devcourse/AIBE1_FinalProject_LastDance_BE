package store.lastdance.service.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import store.lastdance.converter.notification.NotificationSettingConverter;
import store.lastdance.domain.notification.NotificationSetting;
import store.lastdance.domain.notification.NotificationType;
import store.lastdance.domain.user.User;
import store.lastdance.dto.notification.NotificationSettingRequestDTO;
import store.lastdance.dto.notification.NotificationSettingResponseDTO;
import store.lastdance.exception.CustomException;
import store.lastdance.exception.ErrorCode;
import store.lastdance.repository.notification.NotificationSettingRepository;
import store.lastdance.repository.user.UserRepository;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class NotificationSettingV2ServiceImpl implements NotificationSettingV2Service {

    private final NotificationSettingRepository settingRepository;
    private final NotificationSettingConverter notificationSettingConverter;

    @Override
    public NotificationSettingResponseDTO getUserSetting(UUID userId) {
        NotificationSetting setting = settingRepository.findByUserId(userId)
                .orElseGet(() -> settingRepository.save(
                        notificationSettingConverter.toEntity(userId)));

        return notificationSettingConverter.toDto(setting);
    }

    @Override
    public NotificationSettingResponseDTO updateSetting(UUID userId, NotificationSettingRequestDTO request) {
        NotificationSetting setting = settingRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOTIFICATION_SETTING_NOT_FOUND));

        boolean updated = false;
        if (request.getEmailEnabled() != null) {
            setting.updateEmailEnabled(request.getEmailEnabled());
            updated = true;
        }
        if (request.getScheduleReminder() != null) {
            setting.updateScheduleReminder(request.getScheduleReminder());
            updated = true;
        }
        if (request.getPaymentReminder() != null) {
            setting.updatePaymentReminder(request.getPaymentReminder());
            updated = true;
        }
        if (request.getChecklistReminder() != null) {
            setting.updateChecklistReminder(request.getChecklistReminder());
            updated = true;
        }
        if (request.getSseEnabled() != null) {
            setting.updateSSEEnabled(request.getSseEnabled());
            updated = true;
        }

        if (updated) {
            settingRepository.save(setting);
        }
        return notificationSettingConverter.toDto(setting);
    }

    @Override
    public void createDefaultSetting(UUID userId) {
        try {
            settingRepository.save(notificationSettingConverter.toEntity(userId));
        } catch (DataIntegrityViolationException e) {
            throw new CustomException(ErrorCode.NOTIFICATION_SETTING_ALREADY_EXISTS);
        }
    }
}
