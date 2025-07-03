package store.lastdance.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisHealthCheck {

    private final RedisTemplate<String, Object> redisTemplate;

    @PostConstruct
    public void checkRedisConnection() {
        try {
            redisTemplate.opsForValue().set("health-check", "ok");
            String result = (String) redisTemplate.opsForValue().get("health-check");

            if ("ok".equals(result)) {
                log.info("Redis 연결 성공");
            } else {
                log.warn("Redis 연결 불안정");
            }
        } catch (Exception e) {
            log.error("Redis 연결 실패: {}", e.getMessage());
        }

    }
}
