package store.lastdance.controller.notification;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import store.lastdance.dto.notification.WebPushSubscriptionRequest;
import store.lastdance.security.oauth.CustomOAuth2User;
import store.lastdance.service.notification.WebPushService;

import java.util.Map;

@Tag(name = "웹푸시 알림", description = "웹푸시 알림 구독 관리 API")
@RestController
@RequestMapping("/api/v1/notifications/webpush")
@RequiredArgsConstructor
public class WebPushController {

    private final WebPushService webPushService;

    @Operation(summary = "웹푸시 구독 등록", description = "사용자의 웹푸시 알림 구독을 등록합니다.")
    @ApiResponse(responseCode = "200", description = "구독 등록 성공")
    @PostMapping("/subscribe")
    public ResponseEntity<Map<String, String>> subscribe(
            @AuthenticationPrincipal CustomOAuth2User user,
            @RequestBody WebPushSubscriptionRequest request) {

        webPushService.subscribeUser(
                user.getUserId(),
                request.getEndpoint(),
                request.getKeys().getP256dh(),
                request.getKeys().getAuth()
        );

        return ResponseEntity.ok(Map.of("message", "웹푸시 구독이 등록되었습니다."));
    }

    @Operation(summary = "웹푸시 구독 해제", description = "사용자의 웹푸시 알림 구독을 해제합니다.")
    @ApiResponse(responseCode = "200", description = "구독 해제 성공")
    @PostMapping("/unsubscribe")
    public ResponseEntity<Map<String, String>> unsubscribe(
            @AuthenticationPrincipal CustomOAuth2User user) {

        webPushService.unsubscribeUser(user.getUserId());
        return ResponseEntity.ok(Map.of("message", "웹푸시 구독이 해제되었습니다."));
    }
}