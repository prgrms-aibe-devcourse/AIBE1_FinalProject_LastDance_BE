package store.lastdance.repository.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import store.lastdance.domain.user.OAuthProvider;
import store.lastdance.domain.user.User;
import store.lastdance.domain.user.UserRole;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
//@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@EntityScan("store.lastdance.domain")
@EnableJpaRepositories("store.lastdance.repository")
//@Import(QuerydslConfig.class)
class UserRepositoryTest {

    @org.springframework.context.annotation.Configuration
    static class TestJpaConfig {
        @org.springframework.context.annotation.Bean
        public com.querydsl.jpa.impl.JPAQueryFactory jpaQueryFactory(jakarta.persistence.EntityManager entityManager) {
            return new com.querydsl.jpa.impl.JPAQueryFactory(entityManager);
        }
    }

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager em;

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
        em.persistAndFlush(testUser);
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

    @Test
    @DisplayName("QueryDSL - findByNicknameOrEmail 실패 테스트 (둘 다 불일치)")
    void findByNicknameOrEmail_NotFound() {
        Optional<User> found = userRepository.findByNicknameOrEmail("nope", "nope@example.com");
        assertThat(found).isNotPresent();
    }
}
