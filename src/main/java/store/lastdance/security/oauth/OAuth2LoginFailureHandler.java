package store.lastdance.security.oauth;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2LoginFailureHandler implements AuthenticationFailureHandler {

    @Value("${spring.profiles.active}")
    private String activeProfile;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
        log.warn("OAuth2 Login Failure - Exception type: {}", exception.getClass().getSimpleName());
        log.warn("OAuth2 Login Failure - Message: {}", exception.getMessage());
        log.warn("OAuth2 Login Failure - Cause: {}", exception.getCause());

        // 에러 타입 구분
        String errorType = "unknown";
        String message = exception.getMessage();

        if (message != null) {
            if (message.contains("USER_INACTIVE")) {
                errorType = "user_inactive";
            }
        }

        String baseUrl = activeProfile.equals("dev")
                ? "http://localhost:5173"
                : "https://woori-zip.lastdance.store";

        String redirectUri = baseUrl + "/auth/callback?error=" + errorType;
        log.info("OAuth2 실패 리다이렉트: {}", redirectUri);
        response.sendRedirect(redirectUri);
    }
}
