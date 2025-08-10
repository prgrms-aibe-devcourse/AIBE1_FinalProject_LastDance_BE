package store.lastdance.controller.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import store.lastdance.converter.UserConverter;
import store.lastdance.domain.user.User;
import store.lastdance.dto.response.ApiResponse;
import store.lastdance.dto.user.UserResponseDTO;
import store.lastdance.dto.user.UserUpdateRequestDTO;
import store.lastdance.security.oauth.CustomOAuth2User;
import store.lastdance.service.user.UserV2QueryService;
import store.lastdance.service.user.UserV2Service;

import java.util.UUID;

@RestController
@RequestMapping("/api/v2/users")
@RequiredArgsConstructor
@Tag(name = "User", description = "사용자 관리 API")
public class UserV2Controller {

    private final UserV2Service userV2Service;
    private final UserV2QueryService userV2QueryService;
    private final UserConverter userConverter;

    @PatchMapping("/me")
    @Operation(summary = "내 정보 수정", description = "닉네임 수정")
    public ResponseEntity<ApiResponse<UserResponseDTO>> updateMyInfo(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomOAuth2User oAuth2User,
            @Valid @RequestBody UserUpdateRequestDTO requestDTO
    ) {
        UUID userId = oAuth2User.getUserId();
        User updatedUser = userV2Service.updateMyInfo(userId, requestDTO);
        UserResponseDTO dto = userConverter.toResponseDTO(updatedUser);
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    @PostMapping(value = "/me/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "프로필 이미지 업로드", description = "JPG, PNG 형식만 지원, 최대 5MB")
    public ResponseEntity<ApiResponse<UserResponseDTO>> uploadProfileImage(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomOAuth2User oAuth2User,
            @RequestParam("file") MultipartFile file
    ) {
        UUID userId = oAuth2User.getUserId();
        UserResponseDTO dto = userV2Service.updateProfileImage(userId, file);
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    @DeleteMapping("/me/profile-image")
    @Operation(summary = "프로필 이미지 삭제", description = "기본 이미지로 변경")
    public ResponseEntity<ApiResponse<UserResponseDTO>> deleteProfileImage(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomOAuth2User oAuth2User
    ) {
        UUID userId = oAuth2User.getUserId();
        UserResponseDTO dto = userV2Service.deleteProfileImage(userId);
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    @GetMapping("/nickname/check")
    @Operation(summary = "닉네임 중복 확인", description = "사용 가능 여부 체크")
    public ResponseEntity<ApiResponse<Boolean>> checkNickname(
            @RequestParam("nickname") String nickname,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomOAuth2User oAuth2User
    ) {
        UUID userId = oAuth2User.getUserId();
        boolean isAvailable = userV2QueryService.isNicknameAvailable(userId, nickname);
        return ResponseEntity.ok(ApiResponse.success(isAvailable));
    }

    @DeleteMapping("/me")
    @Operation(summary = "계정 비활성화", description = "계정 비활성화 및 토큰 무효화")
    public ResponseEntity<ApiResponse<String>> deactivateAccount(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomOAuth2User oAuth2User,
            @Parameter(hidden = true) HttpServletRequest request,
            @Parameter(hidden = true) HttpServletResponse response
    ) {
        UUID userId = oAuth2User.getUserId();
        userV2Service.deactivateUser(userId, request, response);
        return ResponseEntity.ok(ApiResponse.success("계정이 비활성화되었습니다."));
    }
}