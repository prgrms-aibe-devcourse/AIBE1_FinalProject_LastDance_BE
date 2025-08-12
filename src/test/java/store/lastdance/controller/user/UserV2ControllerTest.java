package store.lastdance.controller.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import store.lastdance.converter.UserConverter;
import store.lastdance.domain.user.User;
import store.lastdance.dto.user.UserResponseDTO;
import store.lastdance.dto.user.UserUpdateRequestDTO;
import store.lastdance.exception.GlobalExceptionHandler;
import store.lastdance.security.oauth.CustomOAuth2User;
import store.lastdance.service.user.UserV2QueryService;
import store.lastdance.service.user.UserV2Service;

import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserV2Controller 테스트")
class UserV2ControllerTest {

    @InjectMocks
    private UserV2Controller sut;

    @Mock
    private UserV2Service userV2Service;
    @Mock
    private UserV2QueryService userV2QueryService;
    @Mock
    private UserConverter userConverter;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private UUID userId;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        userId = UUID.randomUUID();

        // @AuthenticationPrincipal CustomOAuth2User를 처리하는 ArgumentResolver 설정
        mockMvc = MockMvcBuilders.standaloneSetup(sut)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    @Override
                    public boolean supportsParameter(MethodParameter parameter) {
                        return parameter.getParameterAnnotation(AuthenticationPrincipal.class) != null;
                    }

                    @Override
                    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                        return new CustomOAuth2User(userId, "test@test.com", "testuser", "KAKAO", "123", Map.of());
                    }
                })
                .build();
    }

    @Nested
    @DisplayName("내 정보 수정 API 테스트")
    class UpdateMyInfoTest {
        @Test
        @DisplayName("성공 - 내 정보를 정상적으로 수정한다")
        void updateMyInfo_success() throws Exception {
            // given
            UserUpdateRequestDTO requestDTO = new UserUpdateRequestDTO("새로운닉네임", 2000000);
            User mockUser = User.builder()
                    .email("test@test.com")
                    .username("testuser")
                    .nickname("새로운닉네임")
                    .provider(store.lastdance.domain.user.OAuthProvider.KAKAO)
                    .providerId("123")
                    .role(store.lastdance.domain.user.UserRole.USER)
                    .isActive(true)
                    .userBudget(2000000)
                    .build(); // Service가 반환할 Mock User
            UserResponseDTO responseDTO = new UserResponseDTO(userId, "test@test.com", "testuser", "새로운닉네임", null, "KAKAO", true, false, 2000000);

            given(userV2Service.updateMyInfo(any(UUID.class), any(UserUpdateRequestDTO.class))).willReturn(mockUser);
            given(userConverter.toResponseDTO(mockUser)).willReturn(responseDTO);

            // when & then
            mockMvc.perform(patch("/api/v2/users/me")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDTO)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.nickname").value("새로운닉네임"))
                    .andExpect(jsonPath("$.data.monthlyBudget").value(2000000));
        }
    }

    @Nested
    @DisplayName("프로필 이미지 업로드 API 테스트")
    class UploadProfileImageTest {
        @Test
        @DisplayName("성공 - 프로필 이미지를 정상적으로 업로드한다")
        void uploadProfileImage_success() throws Exception {
            // given
            MockMultipartFile file = new MockMultipartFile("file", "profile.jpg", MediaType.IMAGE_JPEG_VALUE, "image_content".getBytes());
            UserResponseDTO responseDTO = new UserResponseDTO(userId, "test@test.com", "testuser", "닉네임", "new_image_url", "KAKAO", true, false, 1000000);

            given(userV2Service.updateProfileImage(any(UUID.class), any(MockMultipartFile.class))).willReturn(responseDTO);

            // when & then
            mockMvc.perform(multipart("/api/v2/users/me/profile-image")
                            .file(file))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.profileImageUrl").value("new_image_url"));
        }
    }

    @Nested
    @DisplayName("프로필 이미지 삭제 API 테스트")
    class DeleteProfileImageTest {
        @Test
        @DisplayName("성공 - 프로필 이미지를 정상적으로 삭제한다")
        void deleteProfileImage_success() throws Exception {
            // given
            UserResponseDTO responseDTO = new UserResponseDTO(userId, "test@test.com", "testuser", "닉네임", null, "KAKAO", true, false, 1000000);
            given(userV2Service.deleteProfileImage(any(UUID.class))).willReturn(responseDTO);

            // when & then
            mockMvc.perform(delete("/api/v2/users/me/profile-image"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.profileImageUrl").isEmpty());
        }
    }

    @Nested
    @DisplayName("닉네임 중복 확인 API 테스트")
    class CheckNicknameTest {
        @Test
        @DisplayName("성공 - 사용 가능한 닉네임이면 true를 반환한다")
        void checkNickname_available() throws Exception {
            // given
            String nickname = "사용가능한닉네임";
            given(userV2QueryService.isNicknameAvailable(any(UUID.class), eq(nickname))).willReturn(true);

            // when & then
            mockMvc.perform(get("/api/v2/users/nickname/check")
                            .param("nickname", nickname))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").value(true));
        }

        @Test
        @DisplayName("성공 - 중복된 닉네임이면 false를 반환한다")
        void checkNickname_duplicate() throws Exception {
            // given
            String nickname = "중복된닉네임";
            given(userV2QueryService.isNicknameAvailable(any(UUID.class), eq(nickname))).willReturn(false);

            // when & then
            mockMvc.perform(get("/api/v2/users/nickname/check")
                            .param("nickname", nickname))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").value(false));
        }
    }

    @Nested
    @DisplayName("계정 비활성화 API 테스트")
    class DeactivateAccountTest {
        @Test
        @DisplayName("성공 - 계정을 정상적으로 비활성화한다")
        void deactivateAccount_success() throws Exception {
            // given
            willDoNothing().given(userV2Service).deactivateUser(any(), any(), any());

            // when & then
            mockMvc.perform(delete("/api/v2/users/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").value("계정이 비활성화되었습니다."));
        }
    }
}
