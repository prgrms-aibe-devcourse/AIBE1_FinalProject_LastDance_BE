package store.lastdance.controller.notification;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.extern.slf4j.Slf4j;
import store.lastdance.dto.response.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import store.lastdance.dto.notification.NotificationSettingRequestDTO;
import store.lastdance.dto.notification.NotificationSettingResponseDTO;
import store.lastdance.exception.CustomException;
import store.lastdance.exception.ErrorCode;
import store.lastdance.security.oauth.CustomOAuth2User;
import store.lastdance.service.notification.NotificationSettingV2Service;

@Slf4j
@RestController
@RequestMapping("/api/v2/notification-settings")
@RequiredArgsConstructor
@Tag(name = "알림 설정", description = "사용자 알림 설정 관리 API")
public class NotificationSettingV2Controller {

    private final NotificationSettingV2Service settingService;

    @GetMapping("/me")
    @Operation(
        summary = "내 알림 설정 조회",
        description = "현재 로그인한 사용자의 알림 설정을 조회합니다."
    )
    public ResponseEntity<ApiResponse<NotificationSettingResponseDTO>> getMySettings(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomOAuth2User user) {
        try{
            if (user == null) throw new CustomException(ErrorCode.TOKEN_NOT_FOUND);
            NotificationSettingResponseDTO setting = settingService.getUserSetting(user.getUserId());
            return ResponseEntity.ok(ApiResponse.success(setting));
        } catch (Exception e) {
            throw new CustomException(ErrorCode.NOTIFICATION_SETTING_FOUND_FAILED);
        }
    }

    @PatchMapping("/me")
    @Operation(
            summary = "내 알림 설정 수정",
            description = "현재 로그인한 사용자의 알림 설정을 수정합니다."
    )
    public ResponseEntity<ApiResponse<NotificationSettingResponseDTO>> updateMySettings(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomOAuth2User user,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "수정할 알림 설정")
            @RequestBody @jakarta.validation.Valid NotificationSettingRequestDTO request) {
        try {
            if (user == null) throw new CustomException(ErrorCode.TOKEN_NOT_FOUND);
            NotificationSettingResponseDTO responseDTO = settingService.updateSetting(user.getUserId(), request);
            return ResponseEntity.ok(ApiResponse.success(responseDTO));
        } catch (Exception e) {
            throw new CustomException(ErrorCode.NOTIFICATION_SETTING_UPDATE_FAILED);
        }
    }
}
