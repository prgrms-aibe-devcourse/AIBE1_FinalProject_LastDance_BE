package store.lastdance.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import store.lastdance.domain.user.OAuthProvider;
import store.lastdance.domain.user.User;
import store.lastdance.security.oauth.CustomOAuth2User;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private User testUser;
    private String secretKey = "MyVeryLongSecretKeyForTestingPurposesOnly123456789";

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(secretKey, 30, 7);
        
        testUser = User.builder()
                .userId(UUID.randomUUID())
                .email("test@example.com")
                .username("testuser")
                .nickname("테스트유저")
                .provider(OAuthProvider.GOOGLE)
                .providerId("google123")
                .build();
    }

    @Test
    @DisplayName("User 엔티티로 Access Token 생성 시 nickname과 provider 포함 확인")
    void generateAccessTokenFromUser_ShouldIncludeNicknameAndProvider() {
        // given & when
        String token = jwtTokenProvider.generateAccessToken(testUser);

        // then
        Claims claims = parseToken(token);
        assertThat(claims.get("nickname", String.class)).isEqualTo("테스트유저");
        assertThat(claims.get("provider", String.class)).isEqualTo("GOOGLE");
        assertThat(claims.get("type", String.class)).isEqualTo("access");
        assertThat(claims.get("email", String.class)).isEqualTo("test@example.com");
        assertThat(claims.get("role", String.class)).isEqualTo("USER");
    }

    @Test
    @DisplayName("Authentication으로 Access Token 생성 시 nickname과 provider 포함 확인")
    void generateAccessTokenFromAuth_ShouldIncludeNicknameAndProvider() {
        // given
        CustomOAuth2User oauth2User = new CustomOAuth2User(
                testUser.getUserId(),
                testUser.getEmail(),
                testUser.getNickname(),
                "GOOGLE",
                "google123",
                Map.of()
        );
        
        // when
        String token = jwtTokenProvider.generateAccessToken(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        oauth2User, null, oauth2User.getAuthorities()
                )
        );

        // then
        Claims claims = parseToken(token);
        assertThat(claims.get("nickname", String.class)).isEqualTo("테스트유저");
        assertThat(claims.get("provider", String.class)).isEqualTo("GOOGLE");
        assertThat(claims.get("type", String.class)).isEqualTo("access");
    }

    @Test
    @DisplayName("Refresh Token 생성 시 nickname과 provider 포함 확인")
    void generateRefreshToken_ShouldIncludeNicknameAndProvider() {
        // given & when
        String token = jwtTokenProvider.generateRefreshToken(testUser);

        // then
        Claims claims = parseToken(token);
        assertThat(claims.get("nickname", String.class)).isEqualTo("테스트유저");
        assertThat(claims.get("provider", String.class)).isEqualTo("GOOGLE");
        assertThat(claims.get("type", String.class)).isEqualTo("refresh");
    }

    @Test
    @DisplayName("토큰에서 Authentication 객체 생성 시 nickname과 provider 정보 포함 확인")
    void getAuthentication_ShouldIncludeNicknameAndProvider() {
        // given
        String token = jwtTokenProvider.generateAccessToken(testUser);

        // when
        Authentication authentication = jwtTokenProvider.getAuthentication(token);

        // then
        CustomOAuth2User principal = (CustomOAuth2User) authentication.getPrincipal();
        assertThat(principal.getNickname()).isEqualTo("테스트유저");
        assertThat(principal.getProvider()).isEqualTo("GOOGLE");
        assertThat(principal.getEmail()).isEqualTo("test@example.com");
        assertThat(principal.getUserId()).isEqualTo(testUser.getUserId());
    }

    // TODO: 토큰 만료 테스트 추가
    // TODO: 잘못된 토큰 테스트 추가
    // TODO: 커스텀 예외 발생 테스트 추가

    private Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(secretKey.getBytes()))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
