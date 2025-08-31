package store.lastdance.repository.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import store.lastdance.config.TestConfig;
import store.lastdance.config.QuerydslConfig;
import store.lastdance.domain.user.OAuthProvider;
import store.lastdance.domain.user.User;
import store.lastdance.domain.user.UserRole;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Disabled("프로젝트 테스트 환경 설정 문제 해결 전까지 임시 비활성화")
@DataJpaTest
@Import({QuerydslConfig.class, TestConfig.class})
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        // Given: 테스트 실행 전에 미리 데이터를 저장합니다.
        testUser = User.builder()
            .email("test@example.com")
            .username("test_username")
            .nickname("testuser")
            .provider(OAuthProvider.KAKAO)
            .providerId("testProviderId")
            .role(UserRole.USER)
            .isActive(true)
            .build();
        userRepository.save(testUser);
    }

    @Test
    @DisplayName("QueryDSL - findByIdWithProfileImage 성공 테스트")
    void findByIdWithProfileImage_Success() {
        // When: 우리가 만든 QueryDSL 메서드를 호출합니다.
        Optional<User> foundUserOpt = userRepository.findByIdWithProfileImage(testUser.getUserId());

        // Then: 결과를 검증합니다.
        assertThat(foundUserOpt).isPresent(); // 유저가 존재하는지 확인
        assertThat(foundUserOpt.get().getUserId()).isEqualTo(testUser.getUserId());
        assertThat(foundUserOpt.get().getNickname()).isEqualTo("testuser");
    }

    @Test
    @DisplayName("QueryDSL - findByNicknameOrEmail 성공 테스트 (닉네임으로)")
    void findByNicknameOrEmail_Success_ByNickname() {
        // When
        Optional<User> foundUserOpt = userRepository.findByNicknameOrEmail("testuser", "wrong@email.com");

        // Then
        assertThat(foundUserOpt).isPresent();
        assertThat(foundUserOpt.get().getNickname()).isEqualTo("testuser");
    }

    @Test
    @DisplayName("QueryDSL - findByNicknameOrEmail 성공 테스트 (이메일로)")
    void findByNicknameOrEmail_Success_ByEmail() {
        // When
        Optional<User> foundUserOpt = userRepository.findByNicknameOrEmail("wrongnickname", "test@example.com");

        // Then
        assertThat(foundUserOpt).isPresent();
        assertThat(foundUserOpt.get().getEmail()).isEqualTo("test@example.com");
    }
}
