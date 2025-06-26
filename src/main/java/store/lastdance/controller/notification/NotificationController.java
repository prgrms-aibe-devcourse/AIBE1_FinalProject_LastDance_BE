package store.lastdance.controller.notification;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
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
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "알림 조회", description = "발송된 알림 기록 조회 API (Redis 기반)")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(
        summary = "내 알림 목록 조회",
        description = "현재 로그인한 사용자의 모든 알림 기록을 조회합니다. 최근 30일간의 데이터만 제공됩니다."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "알림 목록 조회 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = NotificationCache.class),
                examples = @ExampleObject(
                    name = "성공 응답 예시",
                    value = """
                    [
                      {
                        "id": "user123:SCHEDULE:calendar456",
                        "userId": "123e4567-e89b-12d3-a456-426614174000",
                        "type": "SCHEDULE",
                        "title": "[알림] 15분 후 일정: 회의",
                        "content": "안녕하세요!\\n\\n'회의' 일정이 곧 시작됩니다.\\n시작 시간: 2024-12-26 14:00\\n\\n감사합니다.",
                        "relatedId": "456",
                        "sentAt": "2024-12-26T13:45:00"
                      }
                    ]
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "인증되지 않은 사용자",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                    {
                      "error": "Unauthorized",
                      "message": "로그인이 필요합니다."
                    }
                    """
                )
            )
        )
    })
    public ResponseEntity<List<NotificationCache>> getMyNotifications(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomOAuth2User user) {
        
        List<NotificationCache> notifications = notificationService.getUserNotifications(user.getUserId());
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/type/{type}")
    @Operation(
        summary = "타입별 알림 조회",
        description = "특정 타입의 알림만 필터링하여 조회합니다.",
        parameters = @Parameter(
            name = "type",
            description = "알림 타입",
            schema = @Schema(
                allowableValues = {"SCHEDULE", "PAYMENT", "CHECKLIST"},
                example = "SCHEDULE"
            )
        )
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "타입별 알림 조회 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = NotificationCache.class),
                examples = @ExampleObject(
                    name = "일정 알림 예시",
                    value = """
                    [
                      {
                        "id": "user123:SCHEDULE:calendar456",
                        "userId": "123e4567-e89b-12d3-a456-426614174000",
                        "type": "SCHEDULE",
                        "title": "[알림] 15분 후 일정: 회의",
                        "content": "안녕하세요!\\n\\n'회의' 일정이 곧 시작됩니다.\\n시작 시간: 2024-12-26 14:00\\n\\n감사합니다.",
                        "relatedId": "456",
                        "sentAt": "2024-12-26T13:45:00"
                      }
                    ]
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "잘못된 알림 타입",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                    {
                      "error": "Bad Request",
                      "message": "잘못된 알림 타입입니다. SCHEDULE, PAYMENT, CHECKLIST 중 하나를 입력하세요."
                    }
                    """
                )
            )
        ),
        @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    public ResponseEntity<List<NotificationCache>> getNotificationsByType(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomOAuth2User user,
            @PathVariable String type) {
        
        List<NotificationCache> notifications = notificationService.getUserNotificationsByType(
                user.getUserId(), type);
        return ResponseEntity.ok(notifications);
    }
}
