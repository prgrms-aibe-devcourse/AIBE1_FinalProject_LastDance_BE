package store.lastdance.service.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import store.lastdance.domain.user.User;
import store.lastdance.exception.CustomException;
import store.lastdance.exception.ErrorCode;
import store.lastdance.security.JwtTokenProvider;
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

    @Override
    public void refreshToken(HttpServletRequest request, HttpServletResponse response) {
        // 쿠키에서 token 추출
        String token = cookieUtils.getCookieValue(request, "refreshToken").orElse(null);

        // 유효성 검증
        if (token == null) {
            log.warn("리프레시 토큰이 없음");
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        
        if (!jwtTokenProvider.isValid(token)) {
            log.warn("리프레시 토큰이 유효하지 않음");
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        
        if (!jwtTokenProvider.isRefreshToken(token)) {
            log.warn("토큰 타입이 refresh가 아님");
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        // 회원 상태 확인
        UUID userId = jwtTokenProvider.getUserId(token);
        User user = userService.findByActiveUser(userId);

        // 새 토큰 생성
        String newAccessToken = jwtTokenProvider.generateAccessToken(user);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(user);

        // 토큰 쿠키에 저장
        cookieUtils.addTokenCookie(response, "accessToken", newAccessToken);
        cookieUtils.addTokenCookie(response, "refreshToken", newRefreshToken);
    }

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        cookieUtils.removeCookie(response, "accessToken");
        cookieUtils.removeCookie(response, "refreshToken");
    }
}
