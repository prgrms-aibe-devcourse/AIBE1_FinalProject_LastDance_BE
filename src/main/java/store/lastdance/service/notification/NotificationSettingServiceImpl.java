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
        NotificationSetting setting = settingRepository.findByUserId(userId).orElse(null);

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
        NotificationSetting existing = settingRepository.findByUserId(userId).orElse(null);
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

    @Override
    public List<User> emailPermitted() {
        // email_enabled가 true인 사용자 ID들을 조회
        List<UUID> enabledUserIds = settingRepository.findUserIdsByEmailEnabledTrue();
        
        // 디버깅을 위한 로그 추가
        log.info("이메일 알림이 허용된 사용자 ID 수: {}", enabledUserIds.size());
        log.debug("이메일 알림 허용 사용자 ID들: {}", enabledUserIds);
        
        // 해당 ID들로 User 엔티티들을 조회하여 반환
        List<User> users = userRepository.findByUserIdIn(enabledUserIds);
        log.info("조회된 사용자 수: {}", users.size());
        
        return users;
    }
}
