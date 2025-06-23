package store.lastdance.repository.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import store.lastdance.domain.user.OAuthProvider;
import store.lastdance.domain.user.User;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByProviderAndProviderId(OAuthProvider oAuthProvider, String providerId);
    boolean existsByNicknameAndUserIdNot(String nickname, UUID userId);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.profileImageFile WHERE u.userId = :userId")
    Optional<User> findByIdWithProfileImage(@Param("userId") UUID userId);

}
