package store.lastdance.security.oauth;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import store.lastdance.security.JwtTokenProvider;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    @Value("${jwt.access-token-expiration-minutes}")
    private long accessTokenExpireTimeInMinutes;
    @Value("${jwt.refresh-token-expiration-days}")
    private long refreshTokenExpireTimeInDays;
    @Value("${spring.profiles.active}")
    private String activeProfile;

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();
        String redirectUri = activeProfile.equals("dev")
                ? "http://localhost:8080"
                : "https://woori-zip.lastdance.store";

        log.debug("oAuth2User: {}", oAuth2User);

        String accessToken = jwtTokenProvider.generateAccessToken(authentication);
        String refreshToken = jwtTokenProvider.generateRefreshToken(authentication);

        addTokenCookie(response, "accessToken", accessToken, getAccessTokenExpireTimeInSeconds());
        addTokenCookie(response, "refreshToken", refreshToken, getRefreshTokenExpireTimeInSeconds());

        response.sendRedirect(redirectUri);
    }

    private void addTokenCookie(HttpServletResponse response, String tokenName, String token, long maxAge) {
        boolean secure = !activeProfile.equals("dev");

        Cookie cookie = new Cookie(tokenName, token);
        cookie.setHttpOnly(true);
        cookie.setSecure(secure);   // 개발환경에서는 false, 배포시 true로 변경
        cookie.setPath("/");
        cookie.setMaxAge((int) maxAge);
        response.addCookie(cookie);

    }

    private long getAccessTokenExpireTimeInSeconds() {
        return accessTokenExpireTimeInMinutes * 60;
    }

    private long getRefreshTokenExpireTimeInSeconds() {
        return refreshTokenExpireTimeInDays * 24 * 60 * 60;
    }


}
