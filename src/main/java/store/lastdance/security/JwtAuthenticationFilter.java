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
        
        try {
            // CookieUtils에서 토큰 안전하게 가져오기
            String token = cookieUtils.getCookieValue(request, "accessToken").orElse(null);

            if (token != null) {
                // 토큰 유효성 검사
                if (!jwtTokenProvider.isValid(token)) {
                    log.warn("액세스 토큰 유효하지 않음");
                    handleAuthError(response, ErrorCode.INVALID_TOKEN);
                    return;
                }
                
                // 액세스 토큰인지 확인
                if (!jwtTokenProvider.isAccessToken(token)) {
                    log.warn("토큰 타입이 access가 아님");
                    handleAuthError(response, ErrorCode.INVALID_TOKEN);
                    return;
                }
                
                // 토큰 만료 확인
                if (jwtTokenProvider.isTokenExpired(token)) {
                    log.warn("액세스 토큰 만료됨");
                    handleAuthError(response, ErrorCode.EXPIRED_TOKEN);
                    return;
                }
                
                // 인증 정보 설정
                Authentication authentication = jwtTokenProvider.getAuthentication(token);
                if (authentication != null) {
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.debug("인증 성공: {}", authentication.getName());
                }
            } else {
                log.debug("액세스 토큰 없음 - 인증 없이 진행");
            }
            
            filterChain.doFilter(request, response);
            
        } catch (CustomException e) {
            log.warn("JWT authentication failed: {}", e.getMessage());
            handleAuthError(response, e.getErrorCode());
        } catch (ExpiredJwtException e) {
            log.warn("Expired JWT token: {}", e.getMessage());
            handleAuthError(response, ErrorCode.EXPIRED_TOKEN);
        } catch (JwtException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
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
