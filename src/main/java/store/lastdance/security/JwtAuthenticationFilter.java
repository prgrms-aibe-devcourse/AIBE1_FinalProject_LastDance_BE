package store.lastdance.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import store.lastdance.exception.CustomException;
import store.lastdance.util.CookieUtils;

import java.io.IOException;

@RequiredArgsConstructor
@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final CookieUtils cookieUtils;
    private final AuthenticationProcessor authenticationProcessor;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        String method = request.getMethod();

        // JWT 필터를 적용하지 않을 경로들
        return path.equals("/") ||
                path.startsWith("/oauth2/") ||
                path.startsWith("/login/") ||
                path.equals("/api/v1/auth/refresh") ||
                path.startsWith("/swagger-ui/") ||
                path.startsWith("/v3/api-docs/") ||
                path.equals("/swagger-ui.html") ||
                path.startsWith("/actuator/") ||
                path.equals("/error") ||
                path.equals("/favicon.ico");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {
            // 토큰 추출
            String accessToken = cookieUtils.getCookieValue(request, "accessToken").orElse(null);
            String refreshToken = cookieUtils.getCookieValue(request, "refreshToken").orElse(null);

            // 토큰 처리 및 인증 설정
            processTokensAndSetAuthentication(accessToken, refreshToken, response);

        } catch (CustomException e) {
            log.warn("JWT authentication failed: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        } catch (ExpiredJwtException e) {
            log.warn("Expired JWT token: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        } catch (JwtException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        } catch (Exception e) {
            log.error("JWT authentication error: ", e);
            SecurityContextHolder.clearContext();
        }

        // 항상 다음 필터로 진행 (Spring Security가 인증 체크함)
        filterChain.doFilter(request, response);
    }

    /**
     * 토큰 처리 및 인증 설정
     */
    private void processTokensAndSetAuthentication(String accessToken, String refreshToken, HttpServletResponse response) {
        AuthenticationProcessor.ProcessResult result;

        if (accessToken != null) {
            // 액세스 토큰이 있는 경우
            result = authenticationProcessor.processAccessToken(accessToken, refreshToken, response);
            handleProcessResult(result);
        } else if (refreshToken != null) {
            // 액세스 토큰은 없고 리프레시 토큰만 있는 경우 (자동 로그인)
            result = authenticationProcessor.processRefreshTokenOnly(refreshToken, response);
            handleProcessResult(result);
        } else {
            log.debug("토큰 없음 - 인증 없이 진행");
        }
    }

    /**
     * 처리 결과에 따른 인증 설정
     */
    private void handleProcessResult(AuthenticationProcessor.ProcessResult result) {
        if (result.isSuccess()) {
            Authentication authentication = result.getAuthentication();
            if (authentication != null) {
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } else {
            log.debug("토큰 처리 실패: {} - 인증 없이 진행", result.getMessage());
        }
    }
}
