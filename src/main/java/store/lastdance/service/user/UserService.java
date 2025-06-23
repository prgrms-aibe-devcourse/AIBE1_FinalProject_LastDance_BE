package store.lastdance.service.user;

import org.springframework.web.multipart.MultipartFile;
import store.lastdance.domain.user.User;
import store.lastdance.dto.user.UserResponseDTO;
import store.lastdance.dto.user.UserUpdateRequestDTO;

import java.util.UUID;

public interface UserService {

    User findByActiveUser(UUID userid);
    User findByUserId(UUID userId);
    UserResponseDTO getUserWithProfileImage(UUID userId);

    User updateMyInfo(UUID userId, UserUpdateRequestDTO requestDTO);
    UserResponseDTO updateProfileImage(UUID userid, MultipartFile file);
    UserResponseDTO deleteProfileImage(UUID userid);

    boolean isNicknameAvailable(UUID userId, String nickname);
}
