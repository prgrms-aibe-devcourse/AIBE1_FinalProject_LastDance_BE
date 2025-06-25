package store.lastdance.security;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationProcessor {

    private final JwtTokenProvider jwtTokenProvider;
    private final TokenValidationService tokenValidationService;
    private final TokenRefreshService tokenRefreshService;

    /**
     * 토큰 처리 결과
     */
    public static class ProcessResult {
        private final boolean success;
        private final Authentication authentication;
        private final String message;

        private ProcessResult(boolean success, Authentication authentication, String message) {
            this.success = success;
            this.authentication = authentication;
            this.message = message;
        }

        public static ProcessResult success(Authentication authentication) {
            return new ProcessResult(true, authentication, "인증 성공");
        }

        public static ProcessResult failure(String message) {
            return new ProcessResult(false, null, message);
        }

        public boolean isSuccess() {
            return success;
        }

        public Authentication getAuthentication() {
            return authentication;
        }

        public String getMessage() {
            return message;
        }
    }

    /**
     * 액세스 토큰 처리 (하이브리드 방식)
     */
    public ProcessResult processAccessToken(String accessToken, String refreshToken, HttpServletResponse response) {
        try {
            // 액세스 토큰 기본 검증
            if (!tokenValidationService.isValidAccessToken(accessToken)) {
                if (refreshToken != null) {
                    if (log.isTraceEnabled()) {
                        log.trace("액세스 토큰 유효하지 않음 - 리프레시 토큰으로 갱신 시도");
                    }
                    return tryRefreshToken(refreshToken, response);
                } else {
                    log.warn("액세스/리프레시 토큰 모두 문제 - 재로그인 필요");
                    tokenRefreshService.clearAllTokens(response);
                    return ProcessResult.failure("액세스/리프레시 토큰 모두 문제");
                }
            }

            // 액세스 토큰 만료 체크
            if (tokenValidationService.isTokenExpired(accessToken)) {
                if (refreshToken != null) {
                    log.info("액세스 토큰 만료됨, 리프레시 토큰으로 갱신 시도");
                    return tryRefreshToken(refreshToken, response);
                } else {
                    // 액세스 토큰 만료 + 리프레시 토큰 없음 = 재로그인
                    log.warn("액세스 토큰 만료됐는데 리프레시 토큰 없음 - 재로그인 필요");
                    tokenRefreshService.clearAllTokens(response);
                    return ProcessResult.failure("액세스 토큰 만료 + 리프레시 토큰 없음");
                }
            }

            // 리프레시 토큰이 없지만 액세스 토큰이 유효한 경우 - 새 리프레시 토큰 발급
            if (refreshToken == null) {
                TokenRefreshService.RefreshResult result = tokenRefreshService.generateMissingRefreshToken(accessToken, response);
                if (result.isSuccess()) {
                    return ProcessResult.success(result.getAuthentication());
                } else {
                    return ProcessResult.failure(result.getMessage());
                }
            }

            // 인증 정보 설정
            Authentication authentication = jwtTokenProvider.getAuthentication(accessToken);
            if (authentication != null) {
                if (log.isTraceEnabled()) {
                    log.trace("인증 성공: {}", authentication.getName());
                }
                return ProcessResult.success(authentication);
            }

            return ProcessResult.failure("인증 객체 생성 실패");

        } catch (Exception e) {
            log.warn("액세스 토큰 처리 중 오류: {}", e.getMessage());
            if (refreshToken != null) {
                return tryRefreshToken(refreshToken, response);
            } else {
                tokenRefreshService.clearAllTokens(response);
                return ProcessResult.failure("토큰 처리 중 오류: " + e.getMessage());
            }
        }
    }

    /**
     * 리프레시 토큰만 있을 때 처리
     */
    public ProcessResult processRefreshTokenOnly(String refreshToken, HttpServletResponse response) {
        log.info("액세스 토큰 없음, 리프레시 토큰으로 자동 갱신 시도");
        return tryRefreshToken(refreshToken, response);
    }

    /**
     * 리프레시 토큰으로 갱신 시도
     */
    private ProcessResult tryRefreshToken(String refreshToken, HttpServletResponse response) {
        TokenRefreshService.RefreshResult result = tokenRefreshService.refreshByRefreshToken(refreshToken, response);
        
        if (result.isSuccess()) {
            return ProcessResult.success(result.getAuthentication());
        } else {
            return ProcessResult.failure(result.getMessage());
        }
    }
}
