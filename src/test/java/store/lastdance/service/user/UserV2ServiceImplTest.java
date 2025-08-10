package store.lastdance.service.user;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.multipart.MultipartFile;
import store.lastdance.domain.common.ImageFile;
import store.lastdance.domain.user.OAuthProvider;
import store.lastdance.domain.user.User;
import store.lastdance.domain.user.UserRole;
import store.lastdance.dto.user.UserResponseDTO;
import store.lastdance.dto.user.UserUpdateRequestDTO;
import store.lastdance.event.UserDeactivatedEvent;
import store.lastdance.exception.CustomException;
import store.lastdance.exception.ErrorCode;
import store.lastdance.repository.user.UserRepository;
import store.lastdance.service.image.ImageService;

import java.lang.reflect.Field;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserV2ServiceImpl (Command) 테스트")
class UserV2ServiceImplTest {

    @InjectMocks
    private UserV2ServiceImpl sut;

    @Mock
    private UserRepository userRepository;
    @Mock
    private ImageService imageService;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private UserV2QueryService userV2QueryService;

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;

    private User user;
    private UUID userId;

    @BeforeEach
    void setUp() throws Exception {
        userId = UUID.randomUUID();
        user = User.builder()
                .email("test@test.com")
                .username("테스트유저")
                .nickname("테스트닉네임")
                .provider(OAuthProvider.KAKAO)
                .providerId("12345")
                .role(UserRole.USER)
                .isActive(true)
                .build();

        Field userIdField = User.class.getDeclaredField("userId");
        userIdField.setAccessible(true);
        ReflectionUtils.setField(userIdField, user, userId);
    }

    private ImageFile createImageFile(UUID fileId, String originalName, String filePath) {
        return ImageFile.builder()
                .fileId(fileId)
                .originalName(originalName)
                .storedName("stored_" + originalName)
                .filePath(filePath)
                .fileSize(100L)
                .mimeType("image/jpeg")
                .build();
    }

    @Nested
    @DisplayName("updateMyInfo 메서드 테스트")
    class UpdateMyInfoTest {
        @Test
        @DisplayName("성공 - 닉네임과 예산을 정상적으로 수정한다")
        void updateMyInfo_success() {
            // given
            UserUpdateRequestDTO requestDTO = new UserUpdateRequestDTO("새로운닉네임", 2000000);
            given(userV2QueryService.findByActiveUser(userId)).willReturn(user);
            given(userV2QueryService.isNicknameAvailable(userId, "새로운닉네임")).willReturn(true);
            given(userRepository.save(any(User.class))).will(invocation -> invocation.getArgument(0));

            // when
            User updatedUser = sut.updateMyInfo(userId, requestDTO);

            // then
            verify(userV2QueryService).findByActiveUser(userId);
            verify(userV2QueryService).isNicknameAvailable(userId, "새로운닉네임");
            verify(userRepository).save(user);
            assertThat(updatedUser.getNickname()).isEqualTo("새로운닉네임");
            assertThat(updatedUser.getUserBudget()).isEqualTo(2000000);
        }

        @Test
        @DisplayName("실패 - 닉네임이 중복되면 NICKNAME_ALREADY_EXISTS 예외를 던진다")
        void updateMyInfo_fail_nicknameExists() {
            // given
            UserUpdateRequestDTO requestDTO = new UserUpdateRequestDTO("중복된닉네임", null);
            given(userV2QueryService.findByActiveUser(userId)).willReturn(user);
            given(userV2QueryService.isNicknameAvailable(userId, "중복된닉네임")).willReturn(false);

            // when
            CustomException exception = assertThrows(CustomException.class, () -> sut.updateMyInfo(userId, requestDTO));

            // then
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.NICKNAME_ALREADY_EXISTS);
            verify(userRepository, never()).save(any(User.class));
        }
    }

    @Nested
    @DisplayName("updateProfileImage 메서드 테스트")
    class UpdateProfileImageTest {
        private final MultipartFile file = new MockMultipartFile("profile", "profile.jpg", "image/jpeg", new byte[0]);

        @Test
        @DisplayName("성공 - 기존 프로필 이미지 없이 새 이미지를 등록한다")
        void updateProfileImage_success_withoutOldImage() {
            // given
            ImageFile newImageFile = createImageFile(UUID.randomUUID(), "profile.jpg", "s3-url");
            given(userRepository.findByIdWithProfileImage(userId)).willReturn(Optional.of(user));
            given(imageService.uploadImageToS3(any(MultipartFile.class), anyString(), anyInt())).willReturn(newImageFile);

            // when
            UserResponseDTO responseDTO = sut.updateProfileImage(userId, file);

            // then
            verify(userRepository).save(user);
            verify(imageService, never()).deleteImageFromS3(any());
            // DTO의 로직에 맞게 getFilePath()와 비교
            assertThat(responseDTO.profileImageUrl()).isEqualTo(newImageFile.getFilePath());
            assertThat(user.getProfileImageFile()).isEqualTo(newImageFile);
        }

        @Test
        @DisplayName("성공 - 기존 프로필 이미지를 삭제하고 새로운 이미지를 등록한다")
        void updateProfileImage_success_withOldImage() {
            // given
            ImageFile oldImageFile = createImageFile(UUID.randomUUID(), "old.jpg", "s3-old-url");
            user.updateProfileImage(oldImageFile);

            ImageFile newImageFile = createImageFile(UUID.randomUUID(), "new.jpg", "s3-new-url");

            given(userRepository.findByIdWithProfileImage(userId)).willReturn(Optional.of(user));
            given(imageService.uploadImageToS3(any(MultipartFile.class), anyString(), anyInt())).willReturn(newImageFile);
            willDoNothing().given(imageService).deleteImageFromS3(oldImageFile.getFileId());

            // when
            sut.updateProfileImage(userId, file);

            // then
            verify(userRepository).save(user);
            verify(imageService).deleteImageFromS3(oldImageFile.getFileId());
            assertThat(user.getProfileImageFile()).isEqualTo(newImageFile);
        }

        @Test
        @DisplayName("실패 - 이미지 업로드 중 예외 발생 시 롤백 처리를 한다")
        void updateProfileImage_fail_uploadError() {
            // given
            ImageFile newImageFile = createImageFile(UUID.randomUUID(), "new.jpg", "s3-new-url");

            given(userRepository.findByIdWithProfileImage(userId)).willReturn(Optional.of(user));
            given(imageService.uploadImageToS3(any(MultipartFile.class), anyString(), anyInt())).willReturn(newImageFile);
            willThrow(new RuntimeException("S3 Upload Failed")).given(userRepository).save(user);

            // when & then
            assertThrows(RuntimeException.class, () -> sut.updateProfileImage(userId, file));

            // then
            verify(imageService).deleteImageFromS3(newImageFile.getFileId());
        }
    }

    @Nested
    @DisplayName("deleteProfileImage 메서드 테스트")
    class DeleteProfileImageTest {
        @Test
        @DisplayName("성공 - 프로필 이미지를 정상적으로 삭제한다")
        void deleteProfileImage_success() {
            // given
            ImageFile imageFile = createImageFile(UUID.randomUUID(), "old.jpg", "s3-old-url");
            user.updateProfileImage(imageFile);

            given(userRepository.findByIdWithProfileImage(userId)).willReturn(Optional.of(user));
            willDoNothing().given(imageService).deleteImageFromS3(imageFile.getFileId());

            // when
            sut.deleteProfileImage(userId);

            // then
            verify(imageService).deleteImageFromS3(imageFile.getFileId());
            verify(userRepository).save(user);
            assertThat(user.getProfileImageFile()).isNull();
        }
    }

    @Nested
    @DisplayName("deactivateUser 메서드 테스트")
    class DeactivateUserTest {
        @Test
        @DisplayName("성공 - 사용자를 정상적으로 비활성화한다")
        void deactivateUser_success() {
            // given
            given(userV2QueryService.findByActiveUser(userId)).willReturn(user);
            willDoNothing().given(eventPublisher).publishEvent(any(UserDeactivatedEvent.class));

            // when
            sut.deactivateUser(userId, request, response);

            // then
            assertThat(user.getIsActive()).isFalse();
            assertThat(user.getInactivedAt()).isNotNull();
            assertThat(user.getEmail()).startsWith("deleted_");

            verify(eventPublisher).publishEvent(any(UserDeactivatedEvent.class));
            verify(userRepository).save(user);
        }
    }
}