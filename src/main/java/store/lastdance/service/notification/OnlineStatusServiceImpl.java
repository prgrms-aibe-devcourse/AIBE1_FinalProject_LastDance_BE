package store.lastdance.service.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class OnlineStatusServiceImpl implements OnlineStatusService {

    private static final long ONLINE_TTL_SECONDS = 90L;

    private final RedisTemplate<String, String> redisTemplate;

    private String key(UUID userId) {
        return "online:" + userId;
    }

    @Override
    public void setUserOnline(UUID userId) {
        redisTemplate.opsForValue().set(key(userId), "1", ONLINE_TTL_SECONDS, TimeUnit.SECONDS);
    }

    @Override
    public void setUserOffline(UUID userId) {
        redisTemplate.delete(key(userId));
    }

    @Override
    public boolean isUserOnline(UUID userId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key(userId)));
    }

    @Override
    public void refreshOnlineTTL(UUID userId) {
        redisTemplate.expire(key(userId), ONLINE_TTL_SECONDS, TimeUnit.SECONDS);
    }
}
