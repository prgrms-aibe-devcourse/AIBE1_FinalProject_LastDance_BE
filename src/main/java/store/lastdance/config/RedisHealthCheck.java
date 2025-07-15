package store.lastdance.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisHealthCheck {

    private final RedisTemplate<String, Object> redisTemplate;

    @PostConstruct
    public void checkRedisConnection() {
        try {
            redisTemplate.opsForValue().set("health-check", "ok");
            String result = (String) redisTemplate.opsForValue().get("health-check");


        } catch (Exception e) {
        }

    }
}
