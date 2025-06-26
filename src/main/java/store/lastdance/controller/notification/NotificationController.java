package store.lastdance.controller.notification;

import io.swagger.v3.oas.annotations.Operation;
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
@Tag(name = "알림 관리", description = "알림 조회 API")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "내 알림 목록 조회", description = "사용자의 모든 알림 기록을 조회합니다.")
    public ResponseEntity<List<NotificationCache>> getMyNotifications(
            @AuthenticationPrincipal CustomOAuth2User userDetails) {
        
        List<NotificationCache> notifications = notificationService.getUserNotifications(userDetails.getUserId());
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/type/{type}")
    @Operation(summary = "타입별 알림 조회", description = "특정 타입의 알림만 조회합니다. (SCHEDULE, PAYMENT, CHECKLIST)")
    public ResponseEntity<List<NotificationCache>> getNotificationsByType(
            @AuthenticationPrincipal CustomOAuth2User userDetails,
            @PathVariable String type) {
        
        List<NotificationCache> notifications = notificationService.getUserNotificationsByType(
                userDetails.getUserId(), type);
        return ResponseEntity.ok(notifications);
    }
}
