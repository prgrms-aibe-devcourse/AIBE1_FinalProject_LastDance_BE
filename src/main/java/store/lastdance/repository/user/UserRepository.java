package store.lastdance.repository.user;

import org.springframework.data.jpa.repository.JpaRepository;
import store.lastdance.domain.user.OAuthProvider;
import store.lastdance.domain.user.User;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByProviderAndProviderId(OAuthProvider oAuthProvider, String providerId);
}
