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
import store.lastdance.service.notification.sse.SSENotificationV2Service;

import java.util.UUID;

@Tag(name = "SSE 실시간 알림", description = "Server-Sent Events 기반 실시간 알림 API")
@RestController
@RequestMapping("/api/v2/notifications")
@RequiredArgsConstructor
public class SSEV2Controller {

    private final SSENotificationV2Service sseService;

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
}