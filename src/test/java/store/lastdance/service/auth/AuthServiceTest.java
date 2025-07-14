package store.lastdance.service.auth;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;
import store.lastdance.domain.user.User;
import store.lastdance.domain.user.OAuthProvider;
import store.lastdance.exception.CustomException;
import store.lastdance.exception.ErrorCode;
import store.lastdance.security.AuthRedisService;
import store.lastdance.security.JwtTokenProvider;
import store.lastdance.service.notification.SSENotificationService;
import store.lastdance.service.user.UserService;
import store.lastdance.util.CookieUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @InjectMocks
    private AuthServiceImpl authService;

    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private UserService userService;
    @Mock
    private CookieUtils cookieUtils;
    @Mock
    private AuthRedisService authRedisService;
    @Mock
    private SSENotificationService sseNotificationService;

    @Mock
    private HttpServletRequest request;
    private MockHttpServletResponse response;

    private User user;
    private String refreshToken;

    @BeforeEach
    void setUp() {
        response = new MockHttpServletResponse();
        user = User.builder()
                .email("test@test.com")
                .nickname("test")
                .provider(OAuthProvider.KAKAO)
                .providerId("123")
                .username("test")
                .build();
        refreshToken = "test-refresh-token";
    }

    @Test
    @DisplayName("리프레시 토큰 갱신 성공")
    void refreshToken_success() {
        // given
        given(cookieUtils.getCookieValue(request, "refreshToken")).willReturn(Optional.of(refreshToken));
        given(jwtTokenProvider.isValidRefreshToken(refreshToken)).willReturn(true);
        given(jwtTokenProvider.isRefreshToken(refreshToken)).willReturn(true);
        given(jwtTokenProvider.getUserId(refreshToken)).willReturn(user.getUserId());
        given(authRedisService.existsRefreshToken(user.getUserId())).willReturn(true);
        given(authRedisService.getRefreshToken(user.getUserId())).willReturn(refreshToken);
        given(userService.findByActiveUser(user.getUserId())).willReturn(user);
        given(jwtTokenProvider.generateAccessToken(user)).willReturn("new-access-token");
        given(jwtTokenProvider.generateRefreshToken(user)).willReturn("new-refresh-token");

        // when
        authService.refreshToken(request, response);

        // then
        then(authRedisService).should().deleteRefreshToken(user.getUserId());
        then(cookieUtils).should().addTokenCookie(response, "accessToken", "new-access-token");
        then(cookieUtils).should().addTokenCookie(response, "refreshToken", "new-refresh-token");
    }

    @Test
    @DisplayName("리프레시 토큰이 쿠키에 없는 경우 예외 발생")
    void refreshToken_fail_noToken() {
        // given
        given(cookieUtils.getCookieValue(request, "refreshToken")).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> authService.refreshToken(request, response))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.TOKEN_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("유효하지 않은 리프레시 토큰인 경우 예외 발생")
    void refreshToken_fail_invalidToken() {
        // given
        given(cookieUtils.getCookieValue(request, "refreshToken")).willReturn(Optional.of(refreshToken));
        given(jwtTokenProvider.isValidRefreshToken(refreshToken)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> authService.refreshToken(request, response))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.INVALID_REFRESH_TOKEN.getMessage());
    }

    @Test
    @DisplayName("Redis에 리프레시 토큰이 없는 경우 예외 발생")
    void refreshToken_fail_notInRedis() {
        // given
        given(cookieUtils.getCookieValue(request, "refreshToken")).willReturn(Optional.of(refreshToken));
        given(jwtTokenProvider.isValidRefreshToken(refreshToken)).willReturn(true);
        given(jwtTokenProvider.isRefreshToken(refreshToken)).willReturn(true);
        given(jwtTokenProvider.getUserId(refreshToken)).willReturn(user.getUserId());
        given(authRedisService.existsRefreshToken(user.getUserId())).willReturn(false);

        // when & then
        assertThatThrownBy(() -> authService.refreshToken(request, response))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.REFRESH_TOKEN_NOT_IN_REDIS.getMessage());
    }

    @Test
    @DisplayName("Redis의 리프레시 토큰과 일치하지 않는 경우 예외 발생")
    void refreshToken_fail_mismatch() {
        // given
        given(cookieUtils.getCookieValue(request, "refreshToken")).willReturn(Optional.of(refreshToken));
        given(jwtTokenProvider.isValidRefreshToken(refreshToken)).willReturn(true);
        given(jwtTokenProvider.isRefreshToken(refreshToken)).willReturn(true);
        given(jwtTokenProvider.getUserId(refreshToken)).willReturn(user.getUserId());
        given(authRedisService.existsRefreshToken(user.getUserId())).willReturn(true);
        given(authRedisService.getRefreshToken(user.getUserId())).willReturn("different-token");

        // when & then
        assertThatThrownBy(() -> authService.refreshToken(request, response))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.REFRESH_TOKEN_MISMATCH.getMessage());
    }

    @Test
    @DisplayName("로그아웃 성공")
    void logout_success() {
        // given
        given(cookieUtils.getCookieValue(request, "refreshToken")).willReturn(Optional.of(refreshToken));
        given(jwtTokenProvider.isValidRefreshToken(refreshToken)).willReturn(true);
        given(jwtTokenProvider.getUserId(refreshToken)).willReturn(user.getUserId());

        // when
        authService.logout(request, response);

        // then
        then(sseNotificationService).should().disconnectUser(user.getUserId());
        then(cookieUtils).should().removeCookie(response, "accessToken");
        then(cookieUtils).should().removeCookie(response, "refreshToken");
        then(authRedisService).should().deleteRefreshTokenWithTimeout(user.getUserId(), 1);
    }

    @Test
    @DisplayName("리프레시 토큰 없이 로그아웃 성공")
    void logout_success_noRefreshToken() {
        // given
        given(cookieUtils.getCookieValue(request, "refreshToken")).willReturn(Optional.empty());

        // when
        authService.logout(request, response);

        // then
        then(sseNotificationService).should(never()).disconnectUser(any());
        then(cookieUtils).should().removeCookie(response, "accessToken");
        then(cookieUtils).should().removeCookie(response, "refreshToken");
        then(authRedisService).should(never()).deleteRefreshTokenWithTimeout(any(), anyInt());
    }

}
