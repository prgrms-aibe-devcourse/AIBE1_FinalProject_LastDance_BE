package store.lastdance.controller.notification;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import store.lastdance.security.oauth.CustomOAuth2User;
import store.lastdance.dto.notification.NotificationSettingRequestDTO;
import store.lastdance.dto.notification.NotificationSettingResponseDTO;
import store.lastdance.service.notification.NotificationSettingService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notification-settings")
@RequiredArgsConstructor
@Tag(name = "⚙️ 알림 설정", description = "사용자 알림 설정 관리 API")
public class NotificationSettingController {

    private final NotificationSettingService settingService;

    @GetMapping("/{userId}")
    @Operation(
        summary = "특정 사용자 알림 설정 조회",
        description = "관리자용 - 특정 사용자의 알림 설정을 조회합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "알림 설정 조회 성공"),
        @ApiResponse(responseCode = "404", description = "사용자 설정을 찾을 수 없음")
    })
    public ResponseEntity<NotificationSettingResponseDTO> getSetting(
            @Parameter(description = "사용자 ID", example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID userId) {
        NotificationSettingResponseDTO setting = settingService.getUserSetting(userId);
        return ResponseEntity.ok(setting);
    }

    @PutMapping("/{userId}")
    @Operation(
        summary = "특정 사용자 알림 설정 수정",
        description = "관리자용 - 특정 사용자의 알림 설정을 수정합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "알림 설정 수정 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터")
    })
    public ResponseEntity<String> updateSetting(
            @Parameter(description = "사용자 ID", example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID userId,
            @RequestBody NotificationSettingRequestDTO request) {
        settingService.updateSetting(userId, request);
        return ResponseEntity.ok("알림 설정이 성공적으로 업데이트되었습니다.");
    }

    @GetMapping("/me")
    @Operation(
        summary = "내 알림 설정 조회",
        description = "현재 로그인한 사용자의 알림 설정을 조회합니다."
    )
    @ApiResponse(responseCode = "200", description = "내 알림 설정 조회 성공")
    public ResponseEntity<NotificationSettingResponseDTO> getMySettings(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomOAuth2User user) {
        NotificationSettingResponseDTO setting = settingService.getUserSetting(user.getUserId());
        return ResponseEntity.ok(setting);
    }

    @PutMapping("/me")
    @Operation(
        summary = "내 알림 설정 수정",
        description = "현재 로그인한 사용자의 알림 설정을 수정합니다."
    )
    @ApiResponse(responseCode = "200", description = "내 알림 설정 수정 성공")
    public ResponseEntity<String> updateMySettings(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomOAuth2User user,
            @RequestBody NotificationSettingRequestDTO request) {
        settingService.updateSetting(user.getUserId(), request);
        return ResponseEntity.ok("알림 설정이 성공적으로 업데이트되었습니다.");
    }
}
