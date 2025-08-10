package store.lastdance.service.user;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;
import store.lastdance.domain.user.User;
import store.lastdance.dto.user.UserResponseDTO;
import store.lastdance.dto.user.UserUpdateRequestDTO;

import java.util.UUID;

public interface UserV2Service {

    User updateMyInfo(UUID userId, UserUpdateRequestDTO requestDTO);

    UserResponseDTO updateProfileImage(UUID userid, MultipartFile file);

    UserResponseDTO deleteProfileImage(UUID userid);

    void deactivateUser(UUID userId, HttpServletRequest request, HttpServletResponse response);

}
