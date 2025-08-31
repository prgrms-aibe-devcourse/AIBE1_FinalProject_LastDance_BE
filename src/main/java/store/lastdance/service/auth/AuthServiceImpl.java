package store.lastdance.service.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import store.lastdance.domain.user.User;
import store.lastdance.exception.CustomException;
import store.lastdance.exception.ErrorCode;
import store.lastdance.security.AuthRedisService;
import store.lastdance.security.JwtTokenProvider;
import store.lastdance.service.notification.SSENotificationService;
import store.lastdance.service.notification.SSENotificationServiceImpl;
import store.lastdance.service.user.UserService;
import store.lastdance.util.CookieUtils;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserService userService;
    private final CookieUtils cookieUtils;
    private final AuthRedisService authRedisService;
    private final SSENotificationService sseNotificationService;

    @Override
    public void refreshToken(HttpServletRequest request, HttpServletResponse response) {
        String token = cookieUtils.getCookieValue(request, "refreshToken").orElse(null);

        if (token == null) {
            log.warn("리프레시 토큰이 없음");
            throw new CustomException(ErrorCode.TOKEN_NOT_FOUND);
        }

        if (!jwtTokenProvider.isValidRefreshToken(token)) {
            log.warn("리프레시 토큰이 유효하지 않음");
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        if (!jwtTokenProvider.isRefreshToken(token)) {
            log.warn("토큰 타입이 refresh가 아님");
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        UUID userId = jwtTokenProvider.getUserId(token);
        if (!authRedisService.existsRefreshToken(userId)) {
            log.warn("레디스에 저장된 리프레시 토큰 없음");
            throw new CustomException(ErrorCode.REFRESH_TOKEN_NOT_IN_REDIS);
        }

        String storedRefreshToken = authRedisService.getRefreshToken(userId);
        if (!storedRefreshToken.equals(token)) {
            log.warn("레디스의 토큰과 일치하지 않음");
            throw new CustomException(ErrorCode.REFRESH_TOKEN_MISMATCH);
        }

        User user = userService.findByActiveUser(userId);

        authRedisService.deleteRefreshToken(userId);
        log.info("기존 리프레시 토큰 삭제: userId={}", userId);

        String newAccessToken = jwtTokenProvider.generateAccessToken(user);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(user);

        log.info("새로운 토큰 갱신 완료: userId={}", userId);

        cookieUtils.addTokenCookie(response, "accessToken", newAccessToken);
        cookieUtils.addTokenCookie(response, "refreshToken", newRefreshToken);
    }

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = cookieUtils.getCookieValue(request, "refreshToken").orElse(null);

        if (refreshToken != null && jwtTokenProvider.isValidRefreshToken(refreshToken)) {
            try {
                UUID userId = jwtTokenProvider.getUserId(refreshToken);
                sseNotificationService.disconnectUser(userId);
                log.info("로그아웃 완료 - sse 정리됨");
            } catch (Exception e) {
                log.warn("SSE 연결 정리 실패: {}", e.getMessage());
            }
        }

        cookieUtils.removeCookie(response, "accessToken");
        cookieUtils.removeCookie(response, "refreshToken");
        log.info("로그아웃 완료 - 쿠키 삭제됨");

        if (refreshToken != null && jwtTokenProvider.isValidRefreshToken(refreshToken)) {
            try {
                UUID userId = jwtTokenProvider.getUserId(refreshToken);
                authRedisService.deleteRefreshTokenWithTimeout(userId, 1);
            } catch (Exception e) {
                log.warn("Redis 토큰 삭제 실패, 자연 만료 처리: {}", e.getMessage());
            }
        }
    }
}
