package store.lastdance.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AuthRedisService {

    private final RedisTemplate<String, Object> redisTemplate;

    // 리프레시 토큰 저장
    public void saveRefreshToken(UUID userId, String refreshToken, long expireTime) {
        String key = String.format("refresh_token_%s", userId);
        redisTemplate.opsForValue().set(key, refreshToken, expireTime, TimeUnit.SECONDS);
    }

    // 리프레시 토큰 조회
    public String getRefreshToken(UUID userId) {
        try {
            String key = String.format("refresh_token_%s", userId);
            Object value = redisTemplate.opsForValue().get(key);
            return value != null ? value.toString() : null;
        } catch (Exception e) {
            return null; // Redis 오류 시 토큰이 없다고 가정
        }
    }

    // 리프레시 토큰 삭제 (로그아웃)
    public void deleteRefreshToken(UUID userId) {
        try {
            String key = String.format("refresh_token_%s", userId);
            redisTemplate.delete(key);
        } catch (Exception e) {
        }
    }

    // 토큰 존재 여부 확인
    public boolean existsRefreshToken(UUID userId) {
        try {
            String key = String.format("refresh_token_%s", userId);
            return redisTemplate.hasKey(key);
        } catch (Exception e) {
            return false;
        }
    }

    // 이전 토큰 임시 저장 (동시성 문제 해결용)
    public void saveOldRefreshToken(UUID userId, String oldToken, long expireTime) {
        String key = String.format("old_refresh_token_%s", userId);
        redisTemplate.opsForValue().set(key, oldToken, expireTime, TimeUnit.SECONDS);
    }

    // 이전 토큰 확인
    public String getOldRefreshToken(UUID userId) {
        String key = String.format("old_refresh_token_%s", userId);
        Object value = redisTemplate.opsForValue().get(key);
        return value != null ? value.toString() : null;
    }

    // 토큰 검증 (현재 토큰 또는 이전 토큰)
    public boolean isValidStoredToken(UUID userId, String token) {
        String currentToken = getRefreshToken(userId);
        String oldToken = getOldRefreshToken(userId);
        

        
        boolean isCurrentMatch = token.equals(currentToken);
        boolean isOldMatch = token.equals(oldToken);
        

        return isCurrentMatch || isOldMatch;
    }

    // 로그아웃시 조건부 삭제
    public void deleteRefreshTokenWithTimeout(UUID userId, long timeoutSeconds) {
        try {
            String key = String.format("refresh_token_%s", userId);

            // 간단한 타임아웃 처리
            long startTime = System.currentTimeMillis();
            redisTemplate.delete(key);
            long endTime = System.currentTimeMillis();

            if (endTime - startTime > timeoutSeconds * 1000) {
            }
        } catch (Exception e) {
        }
    }
}
