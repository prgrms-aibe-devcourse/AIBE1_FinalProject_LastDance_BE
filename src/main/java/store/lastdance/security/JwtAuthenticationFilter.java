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
import store.lastdance.exception.ErrorCode;
import store.lastdance.dto.common.ErrorResponseDTO;
import store.lastdance.util.CookieUtils;

import java.io.IOException;

@RequiredArgsConstructor
@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final CookieUtils cookieUtils;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) 
            throws ServletException, IOException {
        
        // 이미 인증된 요청인지 확인
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            log.debug("Already authenticated, skipping JWT filter");
            filterChain.doFilter(request, response);
            return;
        }
        
        try {
            log.info("=== JWT Filter Debug ===");
            log.info("Request URI: {}", request.getRequestURI());
            log.info("Thread: {}", Thread.currentThread().getName());
            
            // CookieUtils에서 토큰 안전하게 가져오기
            String token = cookieUtils.getCookieValue(request, "accessToken").orElse(null);
            log.info("AccessToken found: {}", token != null ? "YES" : "NO");

            if (token != null) {
                log.info("Token validation started...");
                
                // 토큰 유효성 검사
                if (!jwtTokenProvider.isValid(token)) {
                    log.warn("액세스 토큰 유효하지 않음");
                    handleAuthError(response, ErrorCode.INVALID_TOKEN);
                    return;
                }
                log.info("Token is valid");
                
                // 액세스 토큰인지 확인
                if (!jwtTokenProvider.isAccessToken(token)) {
                    log.warn("토큰 타입이 access가 아님");
                    handleAuthError(response, ErrorCode.INVALID_TOKEN);
                    return;
                }
                log.info("Token type is access");
                
                // 토큰 만료 확인
                if (jwtTokenProvider.isTokenExpired(token)) {
                    log.warn("액세스 토큰 만료됨");
                    handleAuthError(response, ErrorCode.EXPIRED_TOKEN);
                    return;
                }
                log.info("Token is not expired");
                
                // 인증 정보 설정
                Authentication authentication = jwtTokenProvider.getAuthentication(token);
                log.info("Authentication created: {}", authentication != null ? "YES" : "NO");
                
                if (authentication != null) {
                    log.info("Principal type: {}", authentication.getPrincipal().getClass().getSimpleName());
                    log.info("Principal: {}", authentication.getPrincipal());
                    
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.info("인증 성공: {}", authentication.getName());
                } else {
                    log.error("Authentication is null!");
                }
            } else {
                log.warn("액세스 토큰 없음 - 인증 없이 진행");
            }
            
            filterChain.doFilter(request, response);
            
        } catch (CustomException e) {
            log.error("JWT authentication failed: {}", e.getMessage());
            handleAuthError(response, e.getErrorCode());
        } catch (ExpiredJwtException e) {
            log.error("Expired JWT token: {}", e.getMessage());
            handleAuthError(response, ErrorCode.EXPIRED_TOKEN);
        } catch (JwtException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
            handleAuthError(response, ErrorCode.INVALID_TOKEN);
        } catch (Exception e) {
            log.error("JWT authentication error: ", e);
            handleAuthError(response, ErrorCode.TOKEN_ERROR);
        }
    }
    
    /**
     * 인증 에러 발생 시 JSON 응답 처리
     */
    private void handleAuthError(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType("application/json;charset=UTF-8");
        
        ErrorResponseDTO errorResponse = ErrorResponseDTO.builder()
                .status(errorCode.getHttpStatus().value())
                .error(errorCode.name())
                .message(errorCode.getMessage())
                .path(null) // Filter에서는 path 정보 없음
                .build();
                
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
