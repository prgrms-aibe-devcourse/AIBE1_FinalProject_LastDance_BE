package store.lastdance.service.user;

import store.lastdance.domain.user.User;
import store.lastdance.dto.user.UserResponseDTO;

import java.util.UUID;


public interface UserV2QueryService {
    User findByActiveUser(UUID userid);

    User findByUserId(UUID userId);

    UserResponseDTO getUserWithProfileImage(UUID userId);

    boolean isNicknameAvailable(UUID userId, String nickname);

    void validateUserExists(UUID userId);
}
