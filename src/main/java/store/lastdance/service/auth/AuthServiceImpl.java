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
public class AuthServiceImpl implements AuthService {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserService userService;
    private final CookieUtils cookieUtils;
    private final AuthRedisService authRedisService;
//    private final SSENotificationServiceImpl sseNotificationService;
    private final SSENotificationService sseNotificationService;

    @Override
    public void refreshToken(HttpServletRequest request, HttpServletResponse response) {
        // 쿠키에서 token 추출
        String token = cookieUtils.getCookieValue(request, "refreshToken").orElse(null);

        // 유효성 검증
        if (token == null) {
            throw new CustomException(ErrorCode.TOKEN_NOT_FOUND);
        }


        if (!jwtTokenProvider.isValidRefreshToken(token)) {
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        if (!jwtTokenProvider.isRefreshToken(token)) {
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        UUID userId = jwtTokenProvider.getUserId(token);
        // 레디스에서 토큰 확인
        if (!authRedisService.existsRefreshToken(userId)) {
            throw new CustomException(ErrorCode.REFRESH_TOKEN_NOT_IN_REDIS);
        }

        String storedRefreshToken = authRedisService.getRefreshToken(userId);
        if (!storedRefreshToken.equals(token)) {
            throw new CustomException(ErrorCode.REFRESH_TOKEN_MISMATCH);
        }

        // 회원 상태 확인 (활성/비활성)
        User user = userService.findByActiveUser(userId);

        // 기존 리프레시 토큰 삭제
        authRedisService.deleteRefreshToken(userId);

        // 새 토큰 생성
        String newAccessToken = jwtTokenProvider.generateAccessToken(user);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(user);


        // 토큰 쿠키에 저장
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
            } catch (Exception e) {
            }
        }

        // 쿠키 먼저 삭제 (즉시 로그아웃)
        cookieUtils.removeCookie(response, "accessToken");
        cookieUtils.removeCookie(response, "refreshToken");

        // Redis 삭제는 빠르게 시도
        if (refreshToken != null && jwtTokenProvider.isValidRefreshToken(refreshToken)) {
            try {
                UUID userId = jwtTokenProvider.getUserId(refreshToken);
                authRedisService.deleteRefreshTokenWithTimeout(userId, 1); // 1초 타임아웃
            } catch (Exception e) {
            }
        }
    }
}
