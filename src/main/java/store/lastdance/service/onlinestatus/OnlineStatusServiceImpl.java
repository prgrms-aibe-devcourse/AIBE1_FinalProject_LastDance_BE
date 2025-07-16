package store.lastdance.service.onlinestatus;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OnlineStatusServiceImpl implements OnlineStatusService {

    private static final String ONLINE_USER_KEY = "online_users";
    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public void setUserOnline(UUID userId) {
        redisTemplate.opsForSet().add(ONLINE_USER_KEY, userId.toString());
    }

    @Override
    public void setUserOffline(UUID userId) {
        redisTemplate.opsForSet().remove(ONLINE_USER_KEY, userId.toString());
    }

    @Override
    public boolean isUserOnline(UUID userId) {
        return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(ONLINE_USER_KEY, userId.toString()));
    }
}
