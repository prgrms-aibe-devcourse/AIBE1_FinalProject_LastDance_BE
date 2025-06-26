package store.lastdance.controller.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import store.lastdance.dto.notification.NotificationSettingRequestDTO;
import store.lastdance.dto.notification.NotificationSettingResponseDTO;
import store.lastdance.service.notification.NotificationSettingService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notification-settings")
@RequiredArgsConstructor
public class NotificationSettingController {

    private final NotificationSettingService settingService;

    // 알림 설정 조회 (GET)
    @GetMapping("/{userId}")
    public NotificationSettingResponseDTO getSetting(@PathVariable UUID userId) {
        return settingService.getUserSetting(userId);
    }

    // 알림 설정 저장 또는 수정 (PUT or PATCH)
    @PutMapping("/{userId}")
    public void updateSetting(@PathVariable UUID userId, @RequestBody NotificationSettingRequestDTO request) {
        settingService.updateSetting(userId, request);
    }
}
