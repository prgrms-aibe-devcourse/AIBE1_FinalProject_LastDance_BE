package store.lastdance.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthRedisService {

    private final RedisTemplate<String, Object> redisTemplate;

    // 리프레시 토큰 저장
    public void saveRefreshToken(UUID userId, String refreshToken, long expireTime) {
        String key = String.format("refresh_token_%s", userId);
        redisTemplate.opsForValue().set(key, refreshToken, expireTime, TimeUnit.SECONDS);
        log.info("레디스에 리프레시 토큰 저장 완료: userId={}", userId);
        log.debug("토큰 상세 정보: key={}, 만료시간={}초", key, expireTime);
    }

    // 리프레시 토큰 조회
    public String getRefreshToken(UUID userId) {
        String key = String.format("refresh_token_%s", userId);
        Object value = redisTemplate.opsForValue().get(key);
        return value != null ? value.toString() : null;
    }

    // 리프레시 토큰 삭제 (로그아웃)
    public void deleteRefreshToken(UUID userId) {
        String key = String.format("refresh_token_%s", userId);
        redisTemplate.delete(key);
    }

    // 토큰 존재 여부 확인
    public boolean existsRefreshToken(UUID userId) {
        String key = String.format("refresh_token_%s", userId);
        return redisTemplate.hasKey(key);
    }

    // 이전 토큰 임시 저장 (동시성 문제 해결용)
    public void saveOldRefreshToken(UUID userId, String oldToken, long expireTime) {
        String key = String.format("old_refresh_token_%s", userId);
        redisTemplate.opsForValue().set(key, oldToken, expireTime, TimeUnit.SECONDS);
        log.debug("이전 토큰 임시 저장: userId={}, 만료시간={}초", userId, expireTime);
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
        
        log.debug("토큰 검증 시작: userId={}", userId);
        log.debug("요청 토큰: [{}]", token);
        log.debug("현재 토큰: [{}]", currentToken);
        log.debug("이전 토큰: [{}]", oldToken);
        
        boolean isCurrentMatch = token.equals(currentToken);
        boolean isOldMatch = token.equals(oldToken);
        
        log.debug("현재 토큰 일치: {}, 이전 토큰 일치: {}", isCurrentMatch, isOldMatch);
        
        return isCurrentMatch || isOldMatch;
    }
}
