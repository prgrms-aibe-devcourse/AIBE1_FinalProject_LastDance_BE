package store.lastdance.controller.notification;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import store.lastdance.security.oauth.CustomOAuth2User;
import store.lastdance.service.notification.NotificationV2Service;
import store.lastdance.service.notification.SSENotificationV2Service;

import java.util.Map;
import java.util.UUID;

@Tag(name = "SSE 실시간 알림", description = "Server-Sent Events 기반 실시간 알림 API")
@RestController
@RequestMapping("/api/v2/notifications")
@RequiredArgsConstructor
public class SSEV2Controller {

    private final SSENotificationV2Service sseService;
    private final NotificationV2Service notificationService;

    @Operation(summary = "실시간 알림 스트림 연결", description = "SSE를 통한 실시간 알림 수신 연결을 생성합니다. 응답 헤더 X-Connection-Id로 연결 ID를 확인할 수 있습니다.")
    @ApiResponse(responseCode = "200", description = "스트림 연결 성공")
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> streamNotifications(@AuthenticationPrincipal CustomOAuth2User user) {
        String connectionId = UUID.randomUUID().toString();
        SseEmitter emitter = sseService.createConnection(user.getUserId(), connectionId);

        return ResponseEntity.ok()
                .header("Cache-Control", "no-cache")
                .header("Connection", "keep-alive")
                .header("X-Accel-Buffering", "no")
                .header("X-Connection-Id", connectionId)
                .body(emitter);
    }

    @Operation(summary = "알림 읽음 처리", description = "특정 알림을 읽음 상태로 처리합니다.")
    @ApiResponse(responseCode = "200", description = "읽음 처리 성공")
    @PostMapping("/read/{notificationId}")
    public ResponseEntity<Map<String, String>> markAsRead(
            @AuthenticationPrincipal CustomOAuth2User user,
            @PathVariable String notificationId) {

        notificationService.markNotificationAsRead(user.getUserId(), notificationId);

        return ResponseEntity.ok(Map.of(
                "message", "알림이 읽음 처리되었습니다.",
                "notificationId", notificationId,
                "userId", user.getUserId().toString()
        ));
    }

    @Operation(summary = "알림 읽음 상태 확인", description = "특정 알림의 읽음 상태를 확인합니다.")
    @ApiResponse(responseCode = "200", description = "읽음 상태 확인 성공")
    @GetMapping("/read/{notificationId}")
    public ResponseEntity<Map<String, Object>> checkReadStatus(
            @AuthenticationPrincipal CustomOAuth2User user,
            @PathVariable String notificationId) {

        boolean isRead = notificationService.isNotificationRead(user.getUserId(), notificationId);

        return ResponseEntity.ok(Map.of(
                "notificationId", notificationId,
                "userId", user.getUserId().toString(),
                "isRead", isRead
        ));
    }
}