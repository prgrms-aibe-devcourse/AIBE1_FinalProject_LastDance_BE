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

    // Redis에 리프레시 토큰 저장 (이전 토큰도 유지)
    private void saveRefreshTokenToRedis(UUID userId, String refreshToken) {
        // 이전 토큰이 있다면 30초간 유효하게 유지 (동시성 문제 해결)
        String oldToken = authRedisService.getRefreshToken(userId);
        if (oldToken != null && !oldToken.equals(refreshToken)) {
            authRedisService.saveOldRefreshToken(userId, oldToken, 30); // 30초
        }
        
        // 새 토큰 저장
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
        Claims claims = getClaimsOrThrow(token);
        return UUID.fromString(claims.getSubject());
    }

    // 토큰을 통한 ROLE 추출
    public String getRole(String token) {
        Claims claims = getClaimsOrThrow(token);
        return claims.get("role", String.class);
    }

    // 액세서 토큰 여부
    public boolean isAccessToken(String token) {
        Claims claims = getClaimsOrNull(token);
        return claims != null && "access".equals(claims.get("type"));
    }

    // 리프레시 토큰 여부
    public boolean isRefreshToken(String token) {
        Claims claims = getClaimsOrThrow(token);
        return claims != null && "refresh".equals(claims.get("type"));
    }

    // 토큰 유효성 + 타입 검증
    public boolean isValidAccessToken(String token) {
        return isValid(token) && isAccessToken(token);
    }
    public boolean isValidRefreshToken(String token) {
        return isValid(token) && isRefreshToken(token);
    }

    // 토큰을 통한 Claims 추출
    private Claims getClaimsOrThrow(String token) {
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

    private Claims getClaimsOrNull(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException e) {
            return null;
        }
    }

    // 토큰을 통한 Authentication 추출
    public Authentication getAuthentication(String token) {
        Claims claims = getClaimsOrThrow(token);

        UUID userId = UUID.fromString(claims.getSubject());
        String email = claims.get("email", String.class);
        String nickname = claims.get("nickname", String.class);
        String provider = claims.get("provider", String.class);
        String role = claims.get("role", String.class);

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
        Claims claims = getClaimsOrNull(token);
        if (claims == null) {
            return true;
        }

        Date expiration = getClaimsOrThrow(token).getExpiration();
        return expiration.before(new Date());
    }

    // 토큰을 통한 email 추출
    public String getEmailFromToken(String token) {
        Claims claims = getClaimsOrNull(token);
        return claims != null ? claims.get("email", String.class) : null;
    }

    // 토큰의 남은 만료시간을 초 단위로 반환
    public long getTokenExpireTimeInSeconds(String token) {
        Claims claims = getClaimsOrThrow(token);
        Date expiration = claims.getExpiration();
        long currentTime = System.currentTimeMillis();
        return (expiration.getTime() - currentTime) / 1000;
    }
}
