package store.lastdance.repository.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import store.lastdance.domain.user.OAuthProvider;
import store.lastdance.domain.user.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByProviderAndProviderId(OAuthProvider oAuthProvider, String providerId);

    // 기존 사용자 닉네임 수정시 체크
    boolean existsByNicknameAndUserIdNot(String nickname, UUID userId);

    // 가입시, 닉네임 중복 체크
    boolean existsByNickname(String nickname);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.profileImageFile WHERE u.userId = :userId")
    Optional<User> findByIdWithProfileImage(@Param("userId") UUID userId);

    long countByIsActiveTrue();

    long countByIsBannedTrue();

    long countAllByCreatedAtAfter(LocalDateTime newUserCriteria);

    List<User> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    Page<User> findAll(Specification<User> spec, Pageable pageable);

    /**
     * 사용자 ID 목록으로 사용자들 조회
     */
    List<User> findByUserIdIn(List<UUID> userIds);
}
