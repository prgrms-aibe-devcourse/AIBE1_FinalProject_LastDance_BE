package store.lastdance.controller.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import store.lastdance.domain.user.OAuthProvider;
import store.lastdance.domain.user.User;
import store.lastdance.security.oauth.CustomOAuth2User;
import store.lastdance.service.auth.AuthService;
import store.lastdance.service.user.UserService;

import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AuthController 테스트 클래스
 * 
 * @SpringBootTest를 사용해서 실제 스프링 컨텍스트를 로드하지만
 * MockitoBean으로 서비스 계층은 가짜 객체로 대체해서 테스트합니다.
 * 
 * 실제 응답 코드에 맞춰서 테스트를 작성했습니다:
 * - /refresh: permitAll이므로 200 OK
 * - /logout: 인증 필요하므로 WithMockUser 사용
 * - /me: OAuth2 인증 필요
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "jwt.secret-key=MyVeryLongSecretKeyForTestingPurposesOnly123456789",
    "jwt.access-token-expiration-minutes=30", 
    "jwt.refresh-token-expiration-days=7"
})
@Transactional
@DisplayName("AuthController 테스트")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc; // HTTP 요청 시뮬레이션 도구
    
    @Autowired
    private ObjectMapper objectMapper; // JSON 변환 도구
    
    @MockitoBean
    private AuthService authService; // 가짜 AuthService
    
    @MockitoBean  
    private UserService userService; // 가짜 UserService
    
    private User testUser;
    private CustomOAuth2User testOAuth2User;
    private UUID testUserId;

    @BeforeEach
    void setUp() {
        // 테스트에서 사용할 데이터 준비
        testUserId = UUID.randomUUID();
        
        testUser = User.builder()
                .userId(testUserId)
                .email("test@example.com")
                .username("testuser")
                .nickname("테스트유저")
                .provider(OAuthProvider.GOOGLE)
                .providerId("google123")
                .build();
                
        testOAuth2User = new CustomOAuth2User(
                testUserId,
                "test@example.com", 
                "테스트유저",
                "GOOGLE",
                "google123",
                Map.of("email", "test@example.com")
        );
    }

    @Test
    @DisplayName("토큰 재발급 API 테스트 - 성공")
    void refreshToken_Success() throws Exception {
        // given - AuthService의 refreshToken 메서드가 정상 작동한다고 설정
        doNothing().when(authService).refreshToken(any(), any());
        
        // when & then - POST /api/v1/auth/refresh 요청
        // SecurityConfig에서 permitAll()로 설정되어 있어서 인증 없이도 접근 가능
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk()); // 200 OK 확인
        
        // AuthService의 refreshToken 메서드가 정확히 1번 호출되었는지 검증
        verify(authService, times(1)).refreshToken(any(), any());
    }

    @Test
    @DisplayName("로그아웃 API 테스트 - 인증된 사용자")  
    @WithMockUser // 인증된 사용자로 테스트
    void logout_Success() throws Exception {
        // given - AuthService의 logout 메서드가 정상 작동한다고 설정
        doNothing().when(authService).logout(any(), any());
        
        // when & then - 인증된 상태에서 POST /api/v1/auth/logout 요청
        mockMvc.perform(post("/api/v1/auth/logout")
                        .with(csrf()) // CSRF 토큰 추가
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk()); // 200 OK 확인
        
        // AuthService의 logout 메서드가 정확히 1번 호출되었는지 검증
        verify(authService, times(1)).logout(any(), any());
    }

    @Test
    @DisplayName("로그아웃 API 테스트 - 인증하지 않은 사용자는 로그인 페이지로 리다이렉션")
    void logout_Unauthorized_ShouldRedirect() throws Exception {
        // when & then - 인증하지 않은 상태에서 logout 요청하면 로그인 페이지로 리다이렉션
        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().is3xxRedirection()); // 302 리다이렉션 확인
    }

    @Test
    @DisplayName("내 정보 조회 API 테스트 - OAuth2 로그인 성공")
    void getMe_Success() throws Exception {
        // given - UserService가 User 객체를 반환하도록 설정
        when(userService.findByUserId(testUserId)).thenReturn(testUser);
        
        // when & then - OAuth2 로그인 상태로 GET /api/v1/auth/me 요청
        mockMvc.perform(get("/api/v1/auth/me")
                        .with(oauth2Login().oauth2User(testOAuth2User)) // OAuth2 인증 시뮬레이션
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk()) // 200 OK 확인
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                // JSON 응답의 각 필드값 검증
                .andExpect(jsonPath("$.userId").value(testUserId.toString()))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.nickname").value("테스트유저"))
                .andExpect(jsonPath("$.provider").value("GOOGLE"))
                .andExpect(jsonPath("$.isActive").value(true))
                .andExpect(jsonPath("$.isBanned").value(false));
        
        // UserService의 findByUserId가 정확히 1번 호출되었는지 검증
        verify(userService, times(1)).findByUserId(testUserId);
    }

    @Test
    @DisplayName("내 정보 조회 API 테스트 - 인증하지 않은 사용자는 로그인 페이지로 리다이렉션")
    void getMe_Unauthorized_ShouldRedirect() throws Exception {
        // when & then - 인증하지 않은 상태에서 /me 요청하면 로그인 페이지로 리다이렉션
        mockMvc.perform(get("/api/v1/auth/me")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().is3xxRedirection()); // 302 리다이렉션 확인
    }
}
