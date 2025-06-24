package store.lastdance.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.xml.bind.DatatypeConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import store.lastdance.domain.user.User;
import store.lastdance.exception.CustomException;
import store.lastdance.service.user.UserService;
import store.lastdance.util.CookieUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RequiredArgsConstructor
@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final CookieUtils cookieUtils;
    private final ObjectMapper objectMapper;
    private final AuthRedisService authRedisService;
    private final UserService userService;

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
            // CookieUtils에서 토큰 안전하게 가져오기
            String accessToken = cookieUtils.getCookieValue(request, "accessToken").orElse(null);
            String refreshToken = cookieUtils.getCookieValue(request, "refreshToken").orElse(null);

            if (accessToken != null) {
                // 액세스 토큰이 있는 경우
                if (processAccessToken(accessToken, refreshToken, response)) {
                    // 토큰 처리 성공
                } else {
                    // 토큰 갱신 실패시 인증 없이 진행
                    log.debug("토큰 갱신 실패 - 인증 없이 진행");
                }
            } else if (refreshToken != null) {
                // 액세스 토큰은 없고 리프레시 토큰만 있는 경우 (자동 로그인)
                log.info("액세스 토큰 없음, 리프레시 토큰으로 자동 갱신 시도");
                tryRefreshTokenAutoLogin(refreshToken, response);
            } else {
                log.debug("토큰 없음 - 인증 없이 진행");
            }

        } catch (CustomException e) {
            log.warn("JWT authentication failed: {}", e.getMessage());
            // 인증 실패시 SecurityContext를 비워서 Spring Security가 401을 처리하도록 함
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

    // 리프레시 토큰 자동 갱신 시도
    private boolean tryRefreshTokenAutoLogin(String refreshToken, HttpServletResponse response) {
        log.debug("tryRefreshTokenAutoLogin 시작");
        if (refreshToken == null) {
            log.debug("리프레시 토큰 없음");
            return false;
        }

        try {
            // 리프레시 토큰 유효성 검증
            if (!jwtTokenProvider.isValidRefreshToken(refreshToken)) {
                log.warn("리프레시 토큰이 유효하지 않음");
                return false;
            }

            UUID userId = jwtTokenProvider.getUserId(refreshToken);

            // 레디스에서 토큰 확인
            if (!authRedisService.existsRefreshToken(userId)) {
                log.warn("레디스에 저장된 리프레시 토큰 없음");
                return false;
            }

            String storedRefreshToken = authRedisService.getRefreshToken(userId);

            // 현재 토큰 또는 이전 토큰과 일치하는지 확인 (동시성 문제 해결)
            if (!authRedisService.isValidStoredToken(userId, refreshToken)) {
                log.warn("레디스의 토큰들과 일치하지 않음");
                return false;
            }

            // 회원 상태 확인 (활성/비활성)
            User user = userService.findByActiveUser(userId);

            // 새 토큰 생성 (generateRefreshToken에서 자동으로 Redis에 저장됨)
            String newAccessToken = jwtTokenProvider.generateAccessToken(user);
            String newRefreshToken = jwtTokenProvider.generateRefreshToken(user);

            // 토큰 쿠키에 저장
            cookieUtils.addTokenCookie(response, "accessToken", newAccessToken);
            cookieUtils.addTokenCookie(response, "refreshToken", newRefreshToken);

            // 새로운 액세스 토큰으로 인증 설정
            Authentication authentication = jwtTokenProvider.getAuthentication(newAccessToken);
            if (authentication != null) {
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.info("리프레시 토큰으로 자동 갱신 성공: {}", authentication.getName());
                return true;
            }

        } catch (Exception e) {
            log.warn("리프레시 토큰 자동 갱신 실패: {}", e.getMessage());
        }

        return false;
    }

    private boolean processAccessToken(String accessToken, String refreshToken, HttpServletResponse response) {
        try {
            // 액세스 토큰 기본 검증
            if (!jwtTokenProvider.isValidAccessToken(accessToken)) {
                if (refreshToken != null) {
                    if (log.isTraceEnabled()) {
                        log.trace("액세스 토큰 유효하지 않음 - 리프레시 토큰으로 갱신 시도");
                    }
                    return tryRefreshTokenAutoLogin(refreshToken, response);
                } else {
                    log.warn("액세스/리프레시 토큰 모두 문제 - 재로그인 필요");
                    clearAllTokens(response);
                    return false;
                }
            }

            // 액세스 토큰 만료 체크
            if (jwtTokenProvider.isTokenExpired(accessToken)) {
                if (refreshToken != null) {
                    log.info("액세스 토큰 만료됨, 리프레시 토큰으로 갱신 시도");
                    return tryRefreshTokenAutoLogin(refreshToken, response);
                } else {
                    // 액세스 토큰 만료 + 리프레시 토큰 없음 = 재로그인
                    log.warn("액세스 토큰 만료됐는데 리프레시 토큰 없음 - 재로그인 필요");
                    clearAllTokens(response);
                    return false;
                }
            }

            // 리프레시 토큰이 없지만 액세스 토큰이 유효한 경우 - 새 리프레시 토큰 발급
            if (refreshToken == null) {
                log.info("유효한 액세스 토큰이지만 리프레시 토큰 없음 - 새 리프레시 토큰 발급");
                
                UUID userId = jwtTokenProvider.getUserId(accessToken);
                User user = userService.findByActiveUser(userId);
                String newRefreshToken = jwtTokenProvider.generateRefreshToken(user);
                
                cookieUtils.addTokenCookie(response, "refreshToken", newRefreshToken);
            }

            // 인증 정보 설정
            Authentication authentication = jwtTokenProvider.getAuthentication(accessToken);
            if (authentication != null) {
                SecurityContextHolder.getContext().setAuthentication(authentication);
                if (log.isTraceEnabled()) {
                    log.trace("인증 성공: {}", authentication.getName());
                }
                return true;
            }

            return false;

        } catch (Exception e) {
            log.warn("액세스 토큰 처리 중 오류: {}", e.getMessage());
            if (refreshToken != null) {
                return tryRefreshTokenAutoLogin(refreshToken, response);
            } else {
                clearAllTokens(response);
                return false;
            }
        }
    }

    // 모든 토큰 쿠키 삭제 (재로그인 유도)
    private void clearAllTokens(HttpServletResponse response) {
        cookieUtils.removeCookie(response, "accessToken");
        cookieUtils.removeCookie(response, "refreshToken");
        log.info("모든 토큰 쿠키 삭제 완료 - 재로그인 필요");
    }
}
