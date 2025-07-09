package store.lastdance.security.oauth;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import store.lastdance.security.JwtTokenProvider;
import store.lastdance.util.CookieUtils;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {
    @Value("${spring.profiles.active}")
    String activeProfile;

    private final JwtTokenProvider jwtTokenProvider;
    private final CookieUtils cookieUtils;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();
        log.debug("oAuth2User: {}", oAuth2User);

        String redirectUri = activeProfile.equals("dev")
                ? "http://localhost:5173"
                : "https://woori-zip.lastdance.store";

        String accessToken = jwtTokenProvider.generateAccessToken(authentication);
        String refreshToken = jwtTokenProvider.generateRefreshToken(authentication);

        cookieUtils.addTokenCookie(response, "accessToken", accessToken);
        cookieUtils.addTokenCookie(response, "refreshToken", refreshToken);

        response.sendRedirect(redirectUri);
    }

}
