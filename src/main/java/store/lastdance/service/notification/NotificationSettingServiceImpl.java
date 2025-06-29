package store.lastdance.service.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import store.lastdance.domain.notification.NotificationSetting;
import store.lastdance.dto.notification.NotificationSettingRequestDTO;
import store.lastdance.dto.notification.NotificationSettingResponseDTO;
import store.lastdance.repository.notification.NotificationSettingRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
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

    @Override
    public void createDefaultSetting(UUID userId) {
        // 이미 설정이 있는지 확인
        NotificationSetting existing = settingRepository.findByUserId(userId);
        if (existing != null) {
            log.debug("사용자의 알림 설정이 이미 존재합니다: userId={}", userId);
            return;
        }
        
        // 기본 설정으로 새 레코드 생성
        NotificationSetting defaultSetting = NotificationSetting.builder()
                .userId(userId)
                .build();
        
        settingRepository.save(defaultSetting);
        log.info("사용자 기본 알림 설정 생성 완료: userId={}", userId);
    }
}
