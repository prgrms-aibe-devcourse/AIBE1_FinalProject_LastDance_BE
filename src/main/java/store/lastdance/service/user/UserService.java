package store.lastdance.service.user;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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

    // 삭제 = 계정 비활성화 (소프트 딜리트)
    void deactivateUser(UUID userId, HttpServletRequest request, HttpServletResponse response);

    void validateUserExists(UUID userId);
}
