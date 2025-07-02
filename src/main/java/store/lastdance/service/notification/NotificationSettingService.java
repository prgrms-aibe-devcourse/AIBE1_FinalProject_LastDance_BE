package store.lastdance.service.notification;

import store.lastdance.domain.user.User;
import store.lastdance.dto.notification.NotificationSettingRequestDTO;
import store.lastdance.dto.notification.NotificationSettingResponseDTO;

import java.util.List;
import java.util.UUID;

public interface NotificationSettingService {
    NotificationSettingResponseDTO getUserSetting(UUID userId);
    void updateSetting(UUID userId, NotificationSettingRequestDTO request);
    
    // 새 사용자의 기본 알림 설정 생성
    void createDefaultSetting(UUID userId);

    // 이메일 허용해놓은 사용자 리스트
    List<User> emailPermitted();
}
