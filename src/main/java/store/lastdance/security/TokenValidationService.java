package store.lastdance.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenValidationService {

    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 액세스 토큰이 유효하고 만료되지 않았는지 확인
     */
    public boolean isValidAndNotExpired(String accessToken) {
        if (accessToken == null) {
            return false;
        }
        
        return jwtTokenProvider.isValidAccessToken(accessToken) && 
               !jwtTokenProvider.isTokenExpired(accessToken);
    }

    /**
     * 액세스 토큰이 유효한지만 확인 (만료는 별도 체크)
     */
    public boolean isValidAccessToken(String accessToken) {
        return accessToken != null && jwtTokenProvider.isValidAccessToken(accessToken);
    }

    /**
     * 토큰이 만료되었는지 확인
     */
    public boolean isTokenExpired(String token) {
        return token == null || jwtTokenProvider.isTokenExpired(token);
    }

    /**
     * 리프레시 토큰이 유효한지 확인
     */
    public boolean isValidRefreshToken(String refreshToken) {
        return refreshToken != null && jwtTokenProvider.isValidRefreshToken(refreshToken);
    }
}
