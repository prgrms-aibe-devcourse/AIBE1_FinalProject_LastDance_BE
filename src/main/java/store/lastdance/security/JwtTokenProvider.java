package store.lastdance.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import store.lastdance.domain.user.User;
import store.lastdance.exception.CustomException;
import store.lastdance.exception.ErrorCode;
import store.lastdance.security.oauth.CustomOAuth2User;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long accessTokenValidityInMillis;
    private final long refreshTokenValidityInMillis;
    private final AuthRedisService authRedisService;

    public JwtTokenProvider(
            @Value("${jwt.secret-key}") String secretKey,
            @Value("${jwt.access-token-expiration-minutes}") long accessTokenValidityInMinutes,
            @Value("${jwt.refresh-token-expiration-days}") long refreshTokenValidityInDays,
            AuthRedisService authRedisService
    ) {
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes());
        this.accessTokenValidityInMillis = accessTokenValidityInMinutes * 60 * 1000;
        this.refreshTokenValidityInMillis = refreshTokenValidityInDays * 24 * 60 * 60 * 1000;
        this.authRedisService = authRedisService;
    }

    private SecretKey getKey() {
        return key;
    }

    // 액세스 토큰 생성 - 사용자 기반
    public String generateAccessToken(User user) {
        return createToken(createClaims(
                user.getUserId(),
                user.getEmail(),
                user.getRole().name(),
                user.getNickname(),
                user.getProvider().name(),
                "access"), user.getUserId().toString(), accessTokenValidityInMillis);
    }

    // 액세스 토큰 생성 - 인증 기반
    public String generateAccessToken(Authentication auth) {
        CustomOAuth2User user = (CustomOAuth2User) auth.getPrincipal();
        return createToken(createClaims(
                user.getUserId(),
                user.getEmail(),
                "USER",
                user.getNickname(),
                user.getProvider(),
                "access"), user.getUserId().toString(), accessTokenValidityInMillis);
    }

    // 리프레시 토큰 생성 - 사용자 기반
    public String generateRefreshToken(User user) {
        String refreshToken = createToken(createClaims(
                user.getUserId(),
                user.getEmail(),
                user.getRole().name(),
                user.getNickname(),
                user.getProvider().name(),
                "refresh"), user.getUserId().toString(), refreshTokenValidityInMillis);

        saveRefreshTokenToRedis(user.getUserId(), refreshToken);
        return refreshToken;
    }

    // 리프레시 토큰 생성 - 인증 기반
    public String generateRefreshToken(Authentication auth) {
        CustomOAuth2User user = (CustomOAuth2User) auth.getPrincipal();
        String refreshToken = createToken(createClaims(
                user.getUserId(),
                user.getEmail(),
                "USER",
                user.getNickname(),
                user.getProvider(),
                "refresh"), user.getUserId().toString(), refreshTokenValidityInMillis);

        saveRefreshTokenToRedis(user.getUserId(), refreshToken);
        return refreshToken;
    }

    // Redis에 리프레시 토큰 저장
    private void saveRefreshTokenToRedis(UUID userId, String refreshToken) {
        authRedisService.saveRefreshToken(
                userId,
                refreshToken,
                refreshTokenValidityInMillis / 1000
        );
    }

    // Claims 생성
    private Map<String, Object> createClaims(UUID userId, String email, String role, String nickname, String provider, String type) {
        return Map.of(
                "type", type,
                "userId", userId.toString(),
                "email", email,
                "role", role,
                "nickname", nickname,
                "provider", provider
        );
    }

    // 토큰 생성
    private String createToken(Map<String, Object> claims, String subject, long exp) {
        Date now = new Date();
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + exp))
                .signWith(getKey(), Jwts.SIG.HS256)
                .compact();
    }

    // 토큰 유효 여부
    public boolean isValid(String token) {
        try {
            validateTokenOrThrow(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }

    // 토큰 검증
    private void validateTokenOrThrow(String token) {
        Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token);
    }

    // 토큰을 통한 사용자 아이디 추출
    public UUID getUserId(String token) {
        try {
            return UUID.fromString(getClaims(token).getSubject());
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            throw new CustomException(ErrorCode.EXPIRED_TOKEN);
        } catch (JwtException e) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }
    }

    // 토큰을 통한 ROLE 추출
    public String getRole(String token) {
        try {
            return (String) getClaims(token).get("role");
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            throw new CustomException(ErrorCode.EXPIRED_TOKEN);
        } catch (JwtException e) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }
    }

    // 액세서 토큰 여부
    public boolean isAccessToken(String token) {
        return "access".equals(getClaims(token).get("type"));
    }

    // 리프레시 토큰 여부
    public boolean isRefreshToken(String token) {
        return "refresh".equals(getClaims(token).get("type"));
    }

    // 토큰을 통한 Claims 추출
    private Claims getClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            throw new CustomException(ErrorCode.EXPIRED_TOKEN);
        } catch (JwtException e) {
            throw new CustomException(ErrorCode.TOKEN_PARSING_FAILURE);
        }
    }

    // 토큰을 통한 Authentication 추출
    public Authentication getAuthentication(String token) {
        Claims claims = getClaims(token);

        UUID userId = UUID.fromString(claims.getSubject());
        String email = claims.get("email", String.class);
        String role = claims.get("role", String.class);
        String nickname = claims.get("nickname", String.class);
        String provider = claims.get("provider", String.class);

        Map<String, Object> attributes = Map.of(
                "userId", userId.toString(),
                "email", email,
                "nickname", nickname != null ? nickname : "",
                "provider", provider != null ? provider : ""
        );

        CustomOAuth2User principal = new CustomOAuth2User(
                userId,
                email,
                nickname,
                provider,
                null,
                attributes
        );

        return new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role))
        );
    }

    // 토큰 만료 여부
    public boolean isTokenExpired(String token) {
        try {
            Date expiration = getClaims(token).getExpiration();
            return expiration.before(new Date());
        } catch (Exception e) {
            return true; // 파싱 실패시 만료된 것으로 간주
        }
    }

    // 토큰을 통한 email 추출
    public String getEmailFromToken(String token) {
        try {
            return getClaims(token).get("email", String.class);
        } catch (Exception e) {
            return null;
        }
    }
}
