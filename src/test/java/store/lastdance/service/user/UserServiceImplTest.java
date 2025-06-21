package store.lastdance.service.user;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import store.lastdance.domain.user.User;
import store.lastdance.exception.CustomException;
import store.lastdance.exception.ErrorCode;
import store.lastdance.repository.user.UserRepository;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @InjectMocks
    private UserServiceImpl userService;

    @Mock
    private UserRepository userRepository;

    @Test
    @DisplayName("활성 유저면 정상적으로 반환된다")
    void findByActiveUser_success() {
        // given
        UUID userId = UUID.randomUUID();
        User user = mock(User.class);
        given(user.getIsActive()).willReturn(true);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // when
        User result = userService.findByActiveUser(userId);

        // then
        assertThat(result).isEqualTo(user);
    }

    @Test
    @DisplayName("유저가 비활성(isActive=false)이면 CustomException 발생")
    void findByActiveUser_inactiveUser() {
        // given
        UUID userId = UUID.randomUUID();
        User user = mock(User.class);
        given(user.getIsActive()).willReturn(false);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // when & then
        assertThatThrownBy(() -> userService.findByActiveUser(userId))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.USER_INACTIVE.getMessage());
    }

    @Test
    @DisplayName("유저가 존재하지 않으면 CustomException 발생")
    void findByActiveUser_userNotFound() {
        // given
        UUID userId = UUID.randomUUID();
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.findByActiveUser(userId))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.USER_NOT_FOUND.getMessage());
    }
}
