package store.lastdance.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;
import org.springframework.security.core.Authentication;
import store.lastdance.domain.user.OAuthProvider;
import store.lastdance.domain.user.User;
import store.lastdance.security.oauth.CustomOAuth2User;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)  // 이 줄 추가
class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private User testUser;
    private UUID testUserId;
    private String secretKey = "MyVeryLongSecretKeyForTestingPurposesOnly123456789";

    @Mock
    private AuthRedisService authRedisService;

    @BeforeEach
    void setUp() throws Exception {
        // Mock 설정
        doNothing().when(authRedisService).saveRefreshToken(any(UUID.class), any(String.class), anyLong());
        doNothing().when(authRedisService).saveOldRefreshToken(any(UUID.class), any(String.class), anyLong());

        jwtTokenProvider = new JwtTokenProvider(secretKey, 30, 7, authRedisService);

        // 테스트용 UUID 생성
        testUserId = UUID.randomUUID();

        // User 객체 생성
        testUser = User.builder()
                .email("test@example.com")
                .username("testuser")
                .nickname("테스트유저")
                .provider(OAuthProvider.GOOGLE)
                .providerId("google123")
                .build();

        // 리플렉션을 사용하여 userId 설정
        setUserId(testUser, testUserId);
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
        assertThat(claims.get("userId", String.class)).isEqualTo(testUserId.toString());
    }

    @Test
    @DisplayName("Authentication으로 Access Token 생성 시 nickname과 provider 포함 확인")
    void generateAccessTokenFromAuth_ShouldIncludeNicknameAndProvider() {
        // given
        CustomOAuth2User oauth2User = new CustomOAuth2User(
                testUserId,
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
        assertThat(claims.get("userId", String.class)).isEqualTo(testUserId.toString());
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
        assertThat(claims.get("userId", String.class)).isEqualTo(testUserId.toString());
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
        assertThat(principal.getUserId()).isEqualTo(testUserId);
    }

    // 리플렉션을 사용하여 private 필드 설정
    private void setUserId(User user, UUID userId) throws Exception {
        Field field = User.class.getDeclaredField("userId");
        field.setAccessible(true);
        field.set(user, userId);
    }

    private Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(secretKey.getBytes()))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}