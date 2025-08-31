package store.lastdance.service.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.ReflectionUtils;
import store.lastdance.converter.user.UserConverter;
import store.lastdance.domain.user.OAuthProvider;
import store.lastdance.domain.user.User;
import store.lastdance.domain.user.UserRole;
import store.lastdance.dto.user.UserResponseDTO;
import store.lastdance.exception.CustomException;
import store.lastdance.exception.ErrorCode;
import store.lastdance.repository.user.UserRepository;

import java.lang.reflect.Field;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserV2QueryService 테스트")
class UserV2QueryServiceImplTest {

    @InjectMocks
    private UserV2QueryServiceImpl sut; // System Under Test

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserConverter userConverter;

    private User activeUser;
    private User inactiveUser;
    private UUID userId;

    @BeforeEach
    void setUp() throws NoSuchFieldException {
        userId = UUID.randomUUID();

        activeUser = User.builder()
                .email("active@test.com")
                .username("활성사용자")
                .nickname("활성사용자닉네임")
                .provider(OAuthProvider.KAKAO)
                .providerId("12345")
                .role(UserRole.USER)
                .isActive(true)
                .build();

        inactiveUser = User.builder()
                .email("inactive@test.com")
                .username("비활성사용자")
                .nickname("비활성사용자닉네임")
                .provider(OAuthProvider.KAKAO)
                .providerId("67890")
                .role(UserRole.USER)
                .isActive(false)
                .build();

        // Reflection을 사용하여 private final 필드인 userId 설정
        Field userIdField = User.class.getDeclaredField("userId");
        userIdField.setAccessible(true);
        ReflectionUtils.setField(userIdField, activeUser, userId);
        ReflectionUtils.setField(userIdField, inactiveUser, userId);
    }

    @Nested
    @DisplayName("findByActiveUser 메서드 테스트")
    class FindByActiveUserTest {

        @Test
        @DisplayName("성공 - 활성 사용자를 정상적으로 조회한다")
        void findByActiveUser_success() {
            // given
            given(userRepository.findById(userId)).willReturn(Optional.of(activeUser));

            // when
            User result = sut.findByActiveUser(userId);

            // then
            assertThat(result).isEqualTo(activeUser);
            verify(userRepository).findById(userId);
        }

        @Test
        @DisplayName("실패 - 사용자가 존재하지 않으면 USER_NOT_FOUND 예외를 던진다")
        void findByActiveUser_fail_userNotFound() {
            // given
            given(userRepository.findById(userId)).willReturn(Optional.empty());

            // when
            CustomException exception = assertThrows(CustomException.class, () -> sut.findByActiveUser(userId));

            // then
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
        }

        @Test
        @DisplayName("실패 - 사용자가 비활성 상태이면 USER_INACTIVE 예외를 던진다")
        void findByActiveUser_fail_userInactive() {
            // given
            given(userRepository.findById(userId)).willReturn(Optional.of(inactiveUser));

            // when
            CustomException exception = assertThrows(CustomException.class, () -> sut.findByActiveUser(userId));

            // then
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USER_INACTIVE);
        }
    }

    @Nested
    @DisplayName("findByUserId 메서드 테스트")
    class FindByUserIdTest {
        @Test
        @DisplayName("성공 - 사용자 ID로 사용자를 정상적으로 조회한다 (활성/비활성 무관)")
        void findByUserId_success() {
            // given
            given(userRepository.findById(userId)).willReturn(Optional.of(inactiveUser));

            // when
            User result = sut.findByUserId(userId);

            // then
            assertThat(result).isEqualTo(inactiveUser);
        }

        @Test
        @DisplayName("실패 - 사용자가 존재하지 않으면 USER_NOT_FOUND 예외를 던진다")
        void findByUserId_fail_userNotFound() {
            // given
            given(userRepository.findById(userId)).willReturn(Optional.empty());

            // when
            CustomException exception = assertThrows(CustomException.class, () -> sut.findByUserId(userId));

            // then
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("getUserWithProfileImage 메서드 테스트")
    class GetUserWithProfileImageTest {
        @Test
        @DisplayName("성공 - 프로필 이미지를 포함한 사용자 정보를 DTO로 반환한다")
        void getUserWithProfileImage_success() {
            // given
            UserResponseDTO dto = new UserResponseDTO(
                activeUser.getUserId(),
                activeUser.getEmail(),
                activeUser.getUsername(),
                activeUser.getNickname(),
                null, // profileImageUrl
                activeUser.getProvider().toString(),
                activeUser.getIsActive(),
                activeUser.getIsBanned(),
                activeUser.getUserBudget()
            );
            given(userRepository.findByIdWithProfileImage(userId)).willReturn(Optional.of(activeUser));
            given(userConverter.toResponseDTO(activeUser)).willReturn(dto);

            // when
            UserResponseDTO result = sut.getUserWithProfileImage(userId);

            // then
            assertThat(result.userId()).isEqualTo(activeUser.getUserId());
            assertThat(result.nickname()).isEqualTo(activeUser.getNickname());
            verify(userConverter).toResponseDTO(activeUser);
        }

        @Test
        @DisplayName("실패 - 사용자가 없으면 USER_NOT_FOUND 예외를 던진다")
        void getUserWithProfileImage_fail_userNotFound() {
            // given
            given(userRepository.findByIdWithProfileImage(userId)).willReturn(Optional.empty());

            // when
            CustomException exception = assertThrows(CustomException.class, () -> sut.getUserWithProfileImage(userId));

            // then
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("isNicknameAvailable 메서드 테스트")
    class IsNicknameAvailableTest {
        private final String newNickname = "새로운닉네임";

        @Test
        @DisplayName("성공 - 닉네임이 사용 가능하면 true를 반환한다")
        void isNicknameAvailable_success_true() {
            // given
            given(userRepository.existsByNicknameAndUserIdNot(newNickname, userId)).willReturn(false);

            // when
            boolean result = sut.isNicknameAvailable(userId, newNickname);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("실패 - 닉네임이 이미 존재하면 false를 반환한다")
        void isNicknameAvailable_fail_false() {
            // given
            given(userRepository.existsByNicknameAndUserIdNot(newNickname, userId)).willReturn(true);

            // when
            boolean result = sut.isNicknameAvailable(userId, newNickname);

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("실패 - 닉네임이 null이면 false를 반환한다")
        void isNicknameAvailable_fail_null() {
            // when
            boolean result = sut.isNicknameAvailable(userId, null);

            // then
            assertThat(result).isFalse();
            verify(userRepository, never()).existsByNicknameAndUserIdNot(any(), any());
        }

        @Test
        @DisplayName("실패 - 닉네임이 공백이면 false를 반환한다")
        void isNicknameAvailable_fail_blank() {
            // when
            boolean result = sut.isNicknameAvailable(userId, "  ");

            // then
            assertThat(result).isFalse();
            verify(userRepository, never()).existsByNicknameAndUserIdNot(any(), any());
        }
    }

    @Nested
    @DisplayName("validateUserExists 메서드 테스트")
    class ValidateUserExistsTest {

        @Test
        @DisplayName("성공 - 활성 사용자가 존재하면 예외를 던지지 않는다")
        void validateUserExists_success() {
            // given
            given(userRepository.existsById(userId)).willReturn(true);
            given(userRepository.findById(userId)).willReturn(Optional.of(activeUser));

            // when & then
            assertDoesNotThrow(() -> sut.validateUserExists(userId));
            verify(userRepository).existsById(userId);
            verify(userRepository).findById(userId);
        }

        @Test
        @DisplayName("실패 - 사용자가 존재하지 않으면 USER_NOT_FOUND 예외를 던진다")
        void validateUserExists_fail_userNotFound() {
            // given
            given(userRepository.existsById(userId)).willReturn(false);

            // when
            CustomException exception = assertThrows(CustomException.class, () -> sut.validateUserExists(userId));

            // then
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
            verify(userRepository, never()).findById(userId);
        }

        @Test
        @DisplayName("실패 - 사용자가 비활성 상태이면 USER_INACTIVE 예외를 던진다")
        void validateUserExists_fail_userInactive() {
            // given
            given(userRepository.existsById(userId)).willReturn(true);
            given(userRepository.findById(userId)).willReturn(Optional.of(inactiveUser));

            // when
            CustomException exception = assertThrows(CustomException.class, () -> sut.validateUserExists(userId));

            // then
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USER_INACTIVE);
        }
    }
}
