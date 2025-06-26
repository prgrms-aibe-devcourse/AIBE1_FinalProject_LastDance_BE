package store.lastdance.controller.notification;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications/health")
@RequiredArgsConstructor
@Tag(name = "알림 시스템 헬스체크", description = "Redis 연결 상태 확인")
public class NotificationHealthController {

    private final RedisTemplate<String, Object> redisTemplate;

    @GetMapping
    @Operation(summary = "알림 시스템 상태 확인", description = "Redis 연결 상태와 알림 시스템 동작 상태를 확인합니다.")
    public ResponseEntity<Map<String, Object>> checkHealth() {
        Map<String, Object> status = new HashMap<>();
        
        try {
            // Redis 연결 테스트
            String testKey = "health_check_" + System.currentTimeMillis();
            String testValue = "OK";
            
            redisTemplate.opsForValue().set(testKey, testValue);
            String retrievedValue = (String) redisTemplate.opsForValue().get(testKey);
            redisTemplate.delete(testKey);
            
            boolean redisConnected = testValue.equals(retrievedValue);
            
            status.put("redis_connected", redisConnected);
            status.put("notification_system", "ACTIVE");
            status.put("timestamp", LocalDateTime.now());
            status.put("status", redisConnected ? "HEALTHY" : "UNHEALTHY");
            
            return ResponseEntity.ok(status);
            
        } catch (Exception e) {
            status.put("redis_connected", false);
            status.put("notification_system", "ERROR");
            status.put("error_message", e.getMessage());
            status.put("timestamp", LocalDateTime.now());
            status.put("status", "UNHEALTHY");
            
            return ResponseEntity.status(503).body(status);
        }
    }
}
