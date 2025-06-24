package store.lastdance.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Optional;

@Component
public class CookieUtils {

    @Value("${jwt.access-token-expiration-minutes}")
    private long accessTokenExpireMinutes;

    @Value("${jwt.refresh-token-expiration-days}")
    private long refreshTokenExpireDays;

    @Value("${spring.profiles.active:prod}")
    private String activeProfile;

    public void addTokenCookie(HttpServletResponse response, String name, String value) {
        long maxAgeSeconds;
        if ("accessToken".equals(name)) {
            maxAgeSeconds = accessTokenExpireMinutes * 60;
        } else if ("refreshToken".equals(name)) {
            maxAgeSeconds = refreshTokenExpireDays * 24 * 60 * 60;
        } else {
            throw new IllegalArgumentException("지원하지 않는 토큰 쿠키 이름: " + name);
        }
        
        // 환경별 쿠키 설정
        boolean isDev = "dev".equals(activeProfile);
        String sameSite = isDev ? "Lax" : "None";  // 개발: Lax, 운영: None
        boolean secure = !isDev;  // 개발: false, 운영: true

        ResponseCookie cookie = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secure)
                .path("/")
                .domain(getDomain())
                .maxAge(maxAgeSeconds)
                .sameSite(sameSite)
                .build();

        response.addHeader("Set-Cookie", cookie.toString());
    }

    // 도메인 결정 메서드
    private String getDomain() {
        if ("dev".equals(activeProfile)) {
            return null;
        } else {
            return ".lastdance.store";
        }
    }

    public void removeCookie(HttpServletResponse response, String name) {
        // 환경별 쿠키 설정
        boolean isDev = "dev".equals(activeProfile);
        String sameSite = isDev ? "Lax" : "None";
        boolean secure = !isDev;

        ResponseCookie cookie = ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(secure)
                .path("/")
                .maxAge(0)
                .sameSite(sameSite)
                .build();

        response.addHeader("Set-Cookie", cookie.toString());
    }

    public Optional<Cookie> getCookie(HttpServletRequest request, String name) {
        if (request.getCookies() == null) return Optional.empty();
        return Arrays.stream(request.getCookies())
                .filter(cookie -> name.equals(cookie.getName()))
                .findFirst();
    }

    public Optional<String> getCookieValue(HttpServletRequest request, String name) {
        return getCookie(request, name).map(Cookie::getValue);
    }
}
