package store.lastdance.controller.notification;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
@RequestMapping("/api/v1/notifications/health")
@RequiredArgsConstructor
@Tag(name = "알림 시스템 헬스체크", description = "Redis 연결 상태 및 알림 시스템 모니터링")
public class NotificationHealthController {

    private final RedisTemplate<String, Object> redisTemplate;

    @GetMapping
    @Operation(
        summary = "알림 시스템 상태 확인",
        description = """
        Redis 연결 상태와 알림 시스템의 전반적인 동작 상태를 확인합니다.
        
        **체크 항목:**
        - Redis 연결 상태
        - 데이터 읽기/쓰기 테스트
        - 알림 시스템 활성화 상태
        
        **상태 코드:**
        - `HEALTHY`: 모든 시스템 정상
        - `UNHEALTHY`: Redis 연결 문제 또는 시스템 오류
        """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "시스템 정상 상태",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(
                    example = """
                    {
                      "redis_connected": true,
                      "notification_system": "ACTIVE",
                      "timestamp": "2024-12-26T14:30:00",
                      "status": "HEALTHY"
                    }
                    """
                ),
                examples = @ExampleObject(
                    name = "정상 상태",
                    value = """
                    {
                      "redis_connected": true,
                      "notification_system": "ACTIVE",
                      "timestamp": "2024-12-26T14:30:00",
                      "status": "HEALTHY"
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "503",
            description = "시스템 오류 상태 (Service Unavailable)",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(
                    example = """
                    {
                      "redis_connected": false,
                      "notification_system": "ERROR",
                      "error_message": "Connection refused: redis://localhost:6379",
                      "timestamp": "2024-12-26T14:30:00",
                      "status": "UNHEALTHY"
                    }
                    """
                ),
                examples = @ExampleObject(
                    name = "오류 상태",
                    value = """
                    {
                      "redis_connected": false,
                      "notification_system": "ERROR",
                      "error_message": "Connection refused: redis://localhost:6379",
                      "timestamp": "2024-12-26T14:30:00",
                      "status": "UNHEALTHY"
                    }
                    """
                )
            )
        )
    })
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
