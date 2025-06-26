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
import store.lastdance.domain.notification.NotificationCache;
import store.lastdance.service.notification.NotificationService;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "📧 알림 조회", description = "발송된 알림 기록 조회 API")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(
        summary = "내 알림 목록 조회",
        description = "현재 로그인한 사용자의 모든 알림 기록을 조회합니다. (최근 30일간)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "알림 목록 조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    public ResponseEntity<List<NotificationCache>> getMyNotifications(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomOAuth2User userDetails) {
        
        List<NotificationCache> notifications = notificationService.getUserNotifications(userDetails.getUserId());
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/type/{type}")
    @Operation(
        summary = "타입별 알림 조회",
        description = "특정 타입의 알림만 필터링하여 조회합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "타입별 알림 조회 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 알림 타입"),
        @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    public ResponseEntity<List<NotificationCache>> getNotificationsByType(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomOAuth2User userDetails,
            @Parameter(description = "알림 타입 (SCHEDULE, PAYMENT, CHECKLIST)", example = "SCHEDULE")
            @PathVariable String type) {
        
        List<NotificationCache> notifications = notificationService.getUserNotificationsByType(
                userDetails.getUserId(), type);
        return ResponseEntity.ok(notifications);
    }
}
