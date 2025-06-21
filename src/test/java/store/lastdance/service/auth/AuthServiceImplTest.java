package store.lastdance.service.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import store.lastdance.domain.user.User;
import store.lastdance.exception.InvalidTokenException;
import store.lastdance.security.JwtTokenProvider;
import store.lastdance.service.user.UserService;
import store.lastdance.util.CookieUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @InjectMocks
    private AuthServiceImpl authService;

    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private UserService userService;
    @Mock
    private CookieUtils cookieUtils;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;

    @Test
    @DisplayName("정상적인 refreshToken으로 accessToken/refreshToken 재발급 성공")
    void refreshToken_success() {
        // given - 모의 객체 및 반환값 세팅
        String refreshToken = "valid-refresh-token";
        UUID userId = UUID.randomUUID();
        User user = mock(User.class);

        given(cookieUtils.getCookieValue(request, "refreshToken")).willReturn(Optional.of(refreshToken));
        given(jwtTokenProvider.isValid(refreshToken)).willReturn(true);
        given(jwtTokenProvider.isRefreshToken(refreshToken)).willReturn(true);
        given(jwtTokenProvider.getUserId(refreshToken)).willReturn(userId);
        given(userService.findByActiveUser(userId)).willReturn(user);
        given(jwtTokenProvider.generateAccessToken(user)).willReturn("new-access-token");
        given(jwtTokenProvider.generateRefreshToken(user)).willReturn("new-refresh-token");

        // when & then - 예외가 발생하지 않아야 함
        assertThatCode(() -> authService.refreshToken(request, response))
                .doesNotThrowAnyException();

        // verify - 토큰 쿠키가 정상적으로 저장됐는지 확인
        then(cookieUtils).should().addTokenCookie(response, "accessToken", "new-access-token");
        then(cookieUtils).should().addTokenCookie(response, "refreshToken", "new-refresh-token");
    }

    @Test
    @DisplayName("refreshToken 쿠키가 없을 때 예외 발생")
    void refreshToken_noCookie_throwException() {
        // given - 쿠키값이 없음
        given(cookieUtils.getCookieValue(request, "refreshToken")).willReturn(Optional.empty());

        // when & then - InvalidTokenException 발생해야 함
        assertThatThrownBy(() -> authService.refreshToken(request, response))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("유효하지 않은 리프레시 토큰입니다.");
    }

    @Test
    @DisplayName("refreshToken이 유효하지 않으면 예외 발생")
    void refreshToken_invalidToken_throwException() {
        // given - 토큰이 있지만 유효성 실패
        String refreshToken = "invalid-token";
        given(cookieUtils.getCookieValue(request, "refreshToken")).willReturn(Optional.of(refreshToken));
        given(jwtTokenProvider.isValid(refreshToken)).willReturn(false);

        // when & then - 예외 발생
        assertThatThrownBy(() -> authService.refreshToken(request, response))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    @DisplayName("refreshToken의 타입이 'refresh'가 아니면 예외 발생")
    void refreshToken_wrongType_throwException() {
        // given - 토큰 타입이 잘못된 경우
        String refreshToken = "access-token-instead";
        given(cookieUtils.getCookieValue(request, "refreshToken")).willReturn(Optional.of(refreshToken));
        given(jwtTokenProvider.isValid(refreshToken)).willReturn(true);
        given(jwtTokenProvider.isRefreshToken(refreshToken)).willReturn(false);

        // when & then - 예외 발생
        assertThatThrownBy(() -> authService.refreshToken(request, response))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    @DisplayName("비활성화 유저일 경우 예외 발생")
    void refreshToken_inactiveUser_throwException() {
        // given - 유효한 토큰이지만, 유저가 비활성(탈퇴/정지 등)
        String refreshToken = "valid-refresh-token";
        UUID userId = UUID.randomUUID();

        given(cookieUtils.getCookieValue(request, "refreshToken")).willReturn(Optional.of(refreshToken));
        given(jwtTokenProvider.isValid(refreshToken)).willReturn(true);
        given(jwtTokenProvider.isRefreshToken(refreshToken)).willReturn(true);
        given(jwtTokenProvider.getUserId(refreshToken)).willReturn(userId);
        willThrow(new RuntimeException("유저가 비활성화 상태임")).given(userService).findByActiveUser(userId);

        // when & then - RuntimeException 발생
        assertThatThrownBy(() -> authService.refreshToken(request, response))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("비활성화");
    }

    @Test
    @DisplayName("로그아웃 시 쿠키 삭제가 정상적으로 호출된다")
    void logout_success() {
        // when
        authService.logout(request, response);

        // then
        then(cookieUtils).should().removeCookie(response, "accessToken");
        then(cookieUtils).should().removeCookie(response, "refreshToken");
    }
}
