package store.lastdance.security;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import store.lastdance.domain.user.User;
import store.lastdance.service.user.UserService;
import store.lastdance.util.CookieUtils;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TokenRefreshService {

    private final JwtTokenProvider jwtTokenProvider;
    private final AuthRedisService authRedisService;
    private final UserService userService;
    private final CookieUtils cookieUtils;

    /**
     * 리프레시 토큰으로 새 토큰 발급
     */
    public RefreshResult refreshByRefreshToken(String refreshToken, HttpServletResponse response) {
        if (refreshToken == null) {
            return RefreshResult.failure("리프레시 토큰 없음");
        }

        try {
            // 리프레시 토큰 유효성 검증
            if (!jwtTokenProvider.isValidRefreshToken(refreshToken)) {
                return RefreshResult.failure("리프레시 토큰이 유효하지 않음");
            }

            UUID userId = jwtTokenProvider.getUserId(refreshToken);

            // 레디스에서 토큰 확인
            if (!authRedisService.existsRefreshToken(userId)) {
                return RefreshResult.failure("레디스에 저장된 리프레시 토큰 없음");
            }

            String storedRefreshToken = authRedisService.getRefreshToken(userId);

            // 현재 토큰 또는 이전 토큰과 일치하는지 확인 (동시성 문제 해결)
            if (!authRedisService.isValidStoredToken(userId, refreshToken)) {
                return RefreshResult.failure("레디스의 토큰들과 일치하지 않음");
            }

            // 회원 상태 확인 (활성/비활성)
            User user = userService.findByActiveUser(userId);

            // 새 토큰 생성 (generateRefreshToken에서 자동으로 Redis에 저장됨)
            String newAccessToken = jwtTokenProvider.generateAccessToken(user);
            String newRefreshToken = jwtTokenProvider.generateRefreshToken(user);

            // 토큰 쿠키에 저장
            cookieUtils.addTokenCookie(response, "accessToken", newAccessToken);
            cookieUtils.addTokenCookie(response, "refreshToken", newRefreshToken);

            // 새로운 액세스 토큰으로 인증 설정
            Authentication authentication = jwtTokenProvider.getAuthentication(newAccessToken);
            if (authentication != null) {
                return RefreshResult.success(authentication);
            }

            return RefreshResult.failure("인증 객체 생성 실패");

        } catch (Exception e) {
            return RefreshResult.failure("리프레시 토큰 자동 갱신 실패: " + e.getMessage());
        }
    }

    /**
     * 누락된 리프레시 토큰 새로 발급
     */
    public RefreshResult generateMissingRefreshToken(String accessToken, HttpServletResponse response) {

        try {
            UUID userId = jwtTokenProvider.getUserId(accessToken);
            User user = userService.findByActiveUser(userId);
            String newRefreshToken = jwtTokenProvider.generateRefreshToken(user);
            
            cookieUtils.addTokenCookie(response, "refreshToken", newRefreshToken);
            
            Authentication authentication = jwtTokenProvider.getAuthentication(accessToken);
            return RefreshResult.success(authentication);
            
        } catch (Exception e) {
            return RefreshResult.failure("새 리프레시 토큰 발급 실패: " + e.getMessage());
        }
    }

    /**
     * 모든 토큰 쿠키 삭제
     */
    public void clearAllTokens(HttpServletResponse response) {
        cookieUtils.removeCookie(response, "accessToken");
        cookieUtils.removeCookie(response, "refreshToken");
    }

    /**
     * 토큰 갱신 결과
     */
    public static class RefreshResult {
        private final boolean success;
        private final String message;
        private final Authentication authentication;

        private RefreshResult(boolean success, String message, Authentication authentication) {
            this.success = success;
            this.message = message;
            this.authentication = authentication;
        }

        public static RefreshResult success(Authentication authentication) {
            return new RefreshResult(true, "성공", authentication);
        }

        public static RefreshResult failure(String message) {
            return new RefreshResult(false, message, null);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public Authentication getAuthentication() {
            return authentication;
        }
    }
}
