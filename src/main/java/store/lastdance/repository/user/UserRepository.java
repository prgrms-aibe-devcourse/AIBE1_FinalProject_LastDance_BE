package store.lastdance.repository.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import store.lastdance.domain.user.OAuthProvider;
import store.lastdance.domain.user.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByProviderAndProviderId(OAuthProvider oAuthProvider, String providerId);

    long countByIsActiveTrue();

    long countByIsBannedTrue();

    long countAllByCreatedAtAfter(LocalDateTime newUserCriteria);

    List<User> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    Page<User> findAll(Specification<User> spec, Pageable pageable);
}
