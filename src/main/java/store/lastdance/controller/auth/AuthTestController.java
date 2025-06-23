package store.lastdance.controller.auth;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import store.lastdance.security.JwtTokenProvider;
import store.lastdance.security.oauth.CustomOAuth2User;

import java.util.HashMap;
import java.util.Map;

/**
 * 테스트용 컨트롤러 - 인증 및 예외 처리 확인용
 */
@RestController
@RequestMapping("/api/test")
@Slf4j
@RequiredArgsConstructor
public class AuthTestController {
    
    private final JwtTokenProvider jwtTokenProvider;
    
    @GetMapping("/auth")
    public ResponseEntity<Map<String, Object>> testAuth(Authentication authentication) {
        if (authentication == null) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "인증되지 않은 사용자입니다.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }
        
        CustomOAuth2User user = (CustomOAuth2User) authentication.getPrincipal();
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "인증 성공");
        response.put("userId", user.getUserId().toString());
        response.put("email", user.getEmail());
        response.put("nickname", user.getNickname());
        response.put("provider", user.getProvider());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/validate-token")
    public ResponseEntity<Map<String, Object>> validateToken(HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 쿠키에서 토큰 추출
            String accessToken = extractTokenFromCookie(request, "accessToken");
            String refreshToken = extractTokenFromCookie(request, "refreshToken");
            
            response.put("accessToken", Map.of(
                "present", accessToken != null,
                "valid", accessToken != null && jwtTokenProvider.isValid(accessToken),
                "expired", accessToken != null && jwtTokenProvider.isTokenExpired(accessToken)
            ));
            
            response.put("refreshToken", Map.of(
                "present", refreshToken != null,
                "valid", refreshToken != null && jwtTokenProvider.isValid(refreshToken),
                "expired", refreshToken != null && jwtTokenProvider.isTokenExpired(refreshToken)
            ));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("error", "토큰 검증 중 오류 발생: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    @GetMapping("/token-info")
    public ResponseEntity<Map<String, Object>> getTokenInfo(HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String accessToken = extractTokenFromCookie(request, "accessToken");
            
            if (accessToken == null) {
                response.put("error", "액세스 토큰이 없습니다.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            
            if (!jwtTokenProvider.isValid(accessToken)) {
                response.put("error", "유효하지 않은 토큰입니다.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            
            // 토큰에서 정보 추출
            String userId = jwtTokenProvider.getUserId(accessToken).toString();
            String email = jwtTokenProvider.getEmailFromToken(accessToken);
            
            response.put("userId", userId);
            response.put("email", email);
            response.put("isExpired", jwtTokenProvider.isTokenExpired(accessToken));
            response.put("message", "토큰 정보 조회 성공");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("error", "토큰 정보 조회 중 오류 발생: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    private String extractTokenFromCookie(HttpServletRequest request, String tokenName) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (tokenName.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
