package store.lastdance.security.oauth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;

@Component
@RequiredArgsConstructor

public class OAuth2LoginFailureHandler implements AuthenticationFailureHandler {

    @Value("${spring.profiles.active}")
    private String activeProfile;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {


        // 에러 타입과 메시지 구분
        String errorType = "unknown";
        String errorMessage = "로그인에 실패했습니다.";

        if (exception instanceof OAuth2AuthenticationException) {
            OAuth2Error oauth2Error = ((OAuth2AuthenticationException) exception).getError();
            errorType = oauth2Error.getErrorCode();
            errorMessage = oauth2Error.getDescription() != null ?
                    oauth2Error.getDescription() : errorMessage;
        } else if (exception.getMessage() != null) {
            String message = exception.getMessage();
            if (message.contains("USER_INACTIVE")) {
                errorType = "user_inactive";
                errorMessage = "비활성화된 계정입니다.";
            }
        }

        String baseUrl = activeProfile.equals("dev")
                ? "http://localhost:5173"
                : "https://woori-zip.lastdance.store";

        try {
            String encodedMessage = URLEncoder.encode(errorMessage, "UTF-8");
            String redirectUri = String.format("%s/auth/callback?error=%s&message=%s",
                    baseUrl, errorType, encodedMessage);

            response.sendRedirect(redirectUri);
        } catch (Exception e) {
            String fallbackUri = baseUrl + "/auth/callback?error=redirect_failed";
            response.sendRedirect(fallbackUri);
        }
    }
}