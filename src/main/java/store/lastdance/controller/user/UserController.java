package store.lastdance.controller.user;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import store.lastdance.domain.user.User;
import store.lastdance.dto.response.ApiResponse;
import store.lastdance.dto.user.UserResponseDTO;
import store.lastdance.dto.user.UserUpdateRequestDTO;
import store.lastdance.security.oauth.CustomOAuth2User;
import store.lastdance.service.user.UserService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<UserResponseDTO>> updateMyInfo(
            @AuthenticationPrincipal CustomOAuth2User oAuth2User,
            @Valid @RequestBody UserUpdateRequestDTO requestDTO
    ) {
        UUID userId = oAuth2User.getUserId();
        User updatedUser = userService.updateMyInfo(userId, requestDTO);
        UserResponseDTO dto = UserResponseDTO.from(updatedUser);

        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    @PostMapping(value = "/me/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<UserResponseDTO>> uploadProfileImage(
            @AuthenticationPrincipal CustomOAuth2User oAuth2User,
            @RequestParam("file") MultipartFile file
    ) {
        UUID userId = oAuth2User.getUserId();
        UserResponseDTO dto = userService.updateProfileImage(userId, file);

        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    @DeleteMapping("/me/profile-image")
    public ResponseEntity<ApiResponse<UserResponseDTO>> deleteProfileImage(
            @AuthenticationPrincipal CustomOAuth2User oAuth2User) {

        UUID userId = oAuth2User.getUserId();
        UserResponseDTO dto = userService.deleteProfileImage(userId);
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    @GetMapping("/nickname/check")
    public ResponseEntity<ApiResponse<Boolean>> checkNickname(
            @RequestParam("nickname") String nickname,
            @AuthenticationPrincipal CustomOAuth2User oAuth2User) {

        UUID userId = oAuth2User.getUserId();
        boolean isAvailable = userService.isNicknameAvailable(userId, nickname);

        return ResponseEntity.ok(ApiResponse.success(isAvailable));
    }

    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<String>> deactiveAccount(
            @AuthenticationPrincipal CustomOAuth2User oAuth2User
    ) {
        UUID userId = oAuth2User.getUserId();
        userService.deactivateUser(userId);

        return ResponseEntity.ok(ApiResponse.success("계정이 비활성화되었습니다."));
    }

}
