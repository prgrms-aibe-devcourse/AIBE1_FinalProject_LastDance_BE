package store.lastdance.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import store.lastdance.security.oauth.CustomOAuth2User;
import store.lastdance.security.oauth.OAuth2LoginSuccessHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class OAuth2LoginSuccessHandlerTest {

    private JwtTokenProvider jwtTokenProvider;
    private OAuth2LoginSuccessHandler successHandler;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = mock(JwtTokenProvider.class);
        successHandler = new OAuth2LoginSuccessHandler(
                jwtTokenProvider
        );
        setField(successHandler, "accessTokenExpireTimeInMinutes", 30L);
        setField(successHandler, "refreshTokenExpireTimeInDays", 7L);
        setField(successHandler, "activeProfile", "dev");
    }

    @Test
    void onAuthenticationSuccess_쿠키와_리다이렉트_정상작동() throws Exception {
        // given
        Authentication authentication = mock(Authentication.class);
        CustomOAuth2User oAuth2User = mock(CustomOAuth2User.class);
        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(jwtTokenProvider.generateAccessToken(any(Authentication.class))).thenReturn("access-token-mock");
        when(jwtTokenProvider.generateRefreshToken(any(Authentication.class))).thenReturn("refresh-token-mock");

        HttpServletRequest request = mock(HttpServletRequest.class);
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        successHandler.onAuthenticationSuccess(request, response, authentication);

        // then
        Cookie[] cookies = response.getCookies();
        assertThat(cookies).extracting(Cookie::getName).contains("accessToken", "refreshToken");
        assertThat(cookies).filteredOn(c -> c.getName().equals("accessToken"))
                .extracting(Cookie::getValue)
                .containsExactly("access-token-mock");
        assertThat(cookies).filteredOn(c -> c.getName().equals("refreshToken"))
                .extracting(Cookie::getValue)
                .containsExactly("refresh-token-mock");
        assertThat(response.getRedirectedUrl()).isEqualTo("http://localhost:8080");
    }

    // reflection을 통한 private field 세팅 도우미
    private static void setField(Object target, String field, Object value) {
        try {
            var f = target.getClass().getDeclaredField(field);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
