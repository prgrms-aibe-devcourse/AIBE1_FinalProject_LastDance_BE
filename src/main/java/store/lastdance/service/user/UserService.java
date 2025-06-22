package store.lastdance.service.user;

import store.lastdance.domain.user.User;

import java.util.UUID;

public interface UserService {

    User findByActiveUser(UUID userid);
    User findByUserId(UUID userId);
}
