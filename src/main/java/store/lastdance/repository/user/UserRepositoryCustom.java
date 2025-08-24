package store.lastdance.repository.user;

import store.lastdance.domain.user.User;

import java.util.Optional;
import java.util.UUID;

public interface UserRepositoryCustom {

    Optional<User> findByIdWithProfileImage(UUID userId);

    Optional<User> findByNicknameOrEmail(String nickname, String email);

}
