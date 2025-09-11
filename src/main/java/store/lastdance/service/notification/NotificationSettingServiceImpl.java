package store.lastdance.service.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import store.lastdance.domain.notification.NotificationSetting;
import store.lastdance.domain.user.User;
import store.lastdance.dto.notification.NotificationSettingRequestDTO;
import store.lastdance.dto.notification.NotificationSettingResponseDTO;
import store.lastdance.repository.notification.NotificationSettingRepository;
import store.lastdance.repository.user.UserRepository;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationSettingServiceImpl implements NotificationSettingService {

    private final NotificationSettingRepository settingRepository;
    private final UserRepository userRepository;

    @Override
    public NotificationSettingResponseDTO getUserSetting(UUID userId) {
        NotificationSetting setting = settingRepository.findByUserId(userId).orElse(null);
        if (setting == null) {
            createDefaultSetting(userId);
            setting = settingRepository.findByUserId(userId).orElse(null);
            if (setting == null) {
                return NotificationSettingResponseDTO.builder()
                        .settingId(null)
                        .userId(userId)
                        .emailEnabled(true)
                        .scheduleReminder(true)
                        .paymentReminder(true)
                        .checklistReminder(true)
                        .sseEnabled(true)
                        .createdAt(null)
                        .build();
            }
        }

        return NotificationSettingResponseDTO.builder()
                .settingId(setting.getSettingId())
                .userId(setting.getUserId())
                .emailEnabled(setting.isEmailEnabled())
                .scheduleReminder(setting.isScheduleReminder())
                .paymentReminder(setting.isPaymentReminder())
                .checklistReminder(setting.isChecklistReminder())
                .sseEnabled(setting.isSseEnabled())
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
        log.info("알림 설정 업데이트 완료: userId={}", userId);
    }

    @Override
    public void createDefaultSetting(UUID userId) {
        NotificationSetting existing = settingRepository.findByUserId(userId).orElse(null);
        if (existing != null) {
            log.debug("사용자의 알림 설정이 이미 존재합니다: userId={}", userId);
            return;
        }
        
        try {
            NotificationSetting defaultSetting = NotificationSetting.builder()
                    .userId(userId)
                    .build();
            
            settingRepository.save(defaultSetting);
            log.info("사용자 기본 알림 설정 생성 완료: userId={}, emailEnabled={}, scheduleReminder={}, paymentReminder={}, checklistReminder={}", 
                    userId, 
                    defaultSetting.isEmailEnabled(),
                    defaultSetting.isScheduleReminder(),
                    defaultSetting.isPaymentReminder(),
                    defaultSetting.isChecklistReminder());
        } catch (Exception e) {
            log.error("사용자 기본 알림 설정 생성 실패: userId={}, error={}", userId, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public List<User> emailPermitted() {
        List<UUID> enabledUserIds = settingRepository.findUserIdsByEmailEnabledTrue();
        List<User> users = userRepository.findByUserIdIn(enabledUserIds);
        return users;
    }

    @Override
    public List<User> ssePermitted() {
        List<UUID> enabledUserIds = settingRepository.findUserIdsBySSEEnabledTrue();
        List<User> users = userRepository.findByUserIdIn(enabledUserIds);
        return users;
    }

    @Override
    public boolean getSSEEnabledUserForNotificationType(UUID userId, store.lastdance.domain.notification.NotificationType type) {
        return switch (type) {
            case SCHEDULE -> settingRepository.isSSEEnabledAndScheduleReminderTrue(userId);
            case PAYMENT -> settingRepository.isSSEEnabledAndPaymentReminderTrue(userId);
            case CHECKLIST -> settingRepository.isSSEEnabledAndChecklistReminderTrue(userId);
        };
    }
}
