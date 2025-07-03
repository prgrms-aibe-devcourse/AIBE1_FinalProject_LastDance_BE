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
import store.lastdance.dto.notification.TestNotificationRequestDTO;
import store.lastdance.security.oauth.CustomOAuth2User;
import store.lastdance.service.notification.NotificationService;
import store.lastdance.service.notification.SSENotificationService;

import java.util.Map;

@Tag(name = "SSE 실시간 알림", description = "Server-Sent Events 기반 실시간 알림 API")
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class SSEController {

    private final SSENotificationService sseService;
    private final NotificationService notificationService;

    @Operation(summary = "실시간 알림 스트림 연결", description = "SSE를 통한 실시간 알림 수신 연결을 생성합니다.")
    @ApiResponse(responseCode = "200", description = "스트림 연결 성공")
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> streamNotifications(@AuthenticationPrincipal CustomOAuth2User user) {
        SseEmitter emitter = sseService.createConnection(user.getUserId());
        
        return ResponseEntity.ok()
                .header("Cache-Control", "no-cache")
                .header("Connection", "keep-alive")
                .header("X-Accel-Buffering", "no") // Nginx 버퍼링 비활성화
                .body(emitter);
    }

    @Operation(summary = "SSE 연결 상태 확인", description = "현재 SSE 연결 상태를 확인합니다.")
    @ApiResponse(responseCode = "200", description = "연결 상태 확인 성공")
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getConnectionStatus(@AuthenticationPrincipal CustomOAuth2User user) {
        boolean isOnline = sseService.isUserOnline(user.getUserId());
        int totalConnections = sseService.getActiveConnectionCount();
        
        return ResponseEntity.ok(Map.of(
                "userId", user.getUserId().toString(),
                "isOnline", isOnline,
                "totalActiveConnections", totalConnections,
                "timestamp", java.time.LocalDateTime.now()
        ));
    }

    @Operation(summary = "테스트 알림 전송", description = "하이브리드 알림 시스템을 테스트하기 위한 알림을 전송합니다.")
    @ApiResponse(responseCode = "200", description = "테스트 알림 전송 성공")
    @PostMapping("/test")
    public ResponseEntity<Map<String, String>> sendTestNotification(
            @AuthenticationPrincipal CustomOAuth2User user,
            @RequestBody TestNotificationRequestDTO request) {

        // 테스트 알림 전송
        notificationService.sendTestNotification(
                user.getUserId(),
                store.lastdance.domain.notification.NotificationType.valueOf(request.getType()),
                request.getTitle(),
                request.getContent(),
                request.getRelatedId()
        );

        return ResponseEntity.ok(Map.of(
                "message", "테스트 알림이 전송되었습니다.",
                "type", request.getType(),
                "userId", user.getUserId().toString()
        ));
    }

    @Operation(summary = "알림 읽음 처리", description = "특정 알림을 읽음 상태로 처리합니다.")
    @ApiResponse(responseCode = "200", description = "읽음 처리 성공")
    @PostMapping("/read/{notificationId}")
    public ResponseEntity<Map<String, String>> markAsRead(
            @AuthenticationPrincipal CustomOAuth2User user,
            @PathVariable String notificationId) {

        // 알림 읽음 처리
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