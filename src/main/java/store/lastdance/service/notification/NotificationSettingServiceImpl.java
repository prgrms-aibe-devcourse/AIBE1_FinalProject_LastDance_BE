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
public class NotificationSettingServiceImpl implements NotificationSettingService {

    private final NotificationSettingRepository settingRepository;
    private final UserRepository userRepository;

    @Override
    public NotificationSettingResponseDTO getUserSetting(UUID userId) {
        NotificationSetting setting = settingRepository.findByUserId(userId).orElse(null);
        if (setting == null) {
            // 기본 설정을 생성하고 반환
            createDefaultSetting(userId);
            setting = settingRepository.findByUserId(userId).orElse(null);
            if (setting == null) {
                // 생성 실패 시 기본값으로 응답
                return NotificationSettingResponseDTO.builder()
                        .settingId(null)
                        .userId(userId)
                        .emailEnabled(true)
                        .scheduleReminder(true)
                        .paymentReminder(true)
                        .checklistReminder(true)
                        .sseEnabled(true)
                        .webpushEnabled(false)
                        .createdAt(null)
                        .build();
            }
        }

        return NotificationSettingResponseDTO.builder()
                .settingId(setting.getSettingId())
                .userId(setting.getUserId())
                .emailEnabled(setting.getEmailEnabled() != null ? setting.getEmailEnabled() : true)
                .scheduleReminder(setting.getScheduleReminder() != null ? setting.getScheduleReminder() : true)
                .paymentReminder(setting.getPaymentReminder() != null ? setting.getPaymentReminder() : true)
                .checklistReminder(setting.getChecklistReminder() != null ? setting.getChecklistReminder() : true)
                .sseEnabled(setting.getSseEnabled() != null ? setting.getSseEnabled() : true)
                .webpushEnabled(setting.getWebpushEnabled() != null ? setting.getWebpushEnabled() : false) // 웹푸시는 기본값 false
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

        // null 체크를 통해 명시적으로 설정된 값만 업데이트
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
        if (request.getWebpushEnabled() != null) {
            setting.updateWebPushEnabled(request.getWebpushEnabled());
        }

        settingRepository.save(setting);
    }

    @Override
    public void createDefaultSetting(UUID userId) {
        // 이미 설정이 있는지 확인
        NotificationSetting existing = settingRepository.findByUserId(userId).orElse(null);
        if (existing != null) {
            return;
        }
        
        try {
            // 기본 설정으로 새 레코드 생성
            NotificationSetting defaultSetting = NotificationSetting.builder()
                    .userId(userId)
                    .build();
            
            settingRepository.save(defaultSetting);

        } catch (Exception e) {
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
    public List<User> webPushPermitted() {
        List<UUID> enabledUserIds = settingRepository.findUserIdsByWebPushEnabledTrue();
        List<User> users = userRepository.findByUserIdIn(enabledUserIds);
        return users;
    }

    // 통합 메서드 구현
    @Override
    public boolean getSSEEnabledUserForNotificationType(UUID userId, store.lastdance.domain.notification.NotificationType type) {
        return switch (type) {
            case SCHEDULE -> settingRepository.isSSEEnabledAndScheduleReminderTrue(userId);
            case PAYMENT -> settingRepository.isSSEEnabledAndPaymentReminderTrue(userId);
            case CHECKLIST -> settingRepository.isSSEEnabledAndChecklistReminderTrue(userId);
        };
    }

    @Override
    public boolean getWebPushEnabledUserForNotificationType(UUID userId, store.lastdance.domain.notification.NotificationType type) {
        return switch (type) {
            case SCHEDULE -> settingRepository.isWebPushEnabledAndScheduleReminderTrue(userId);
            case PAYMENT -> settingRepository.isWebPushEnabledAndPaymentReminderTrue(userId);
            case CHECKLIST -> settingRepository.isWebPushEnabledAndChecklistReminderTrue(userId);
        };
    }
}
