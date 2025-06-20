package store.lastdance.repository.user;

import org.springframework.data.jpa.repository.JpaRepository;
import store.lastdance.domain.user.User;

import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
}
