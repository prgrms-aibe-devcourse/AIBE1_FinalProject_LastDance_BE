package store.lastdance.controller.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import store.lastdance.dto.user.UserResponseDTO;
import store.lastdance.security.oauth.CustomOAuth2User;
import store.lastdance.service.auth.AuthService;
import store.lastdance.service.user.UserV2QueryService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v2/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "인증 관련 API")
public class AuthV2Controller {

    private final AuthService authService;
    private final UserV2QueryService userV2QueryService;

    @PostMapping("/refresh")
    @Operation(summary = "토큰 갱신", description = "리프레시 토큰으로 새 액세스 토큰 발급")
    public ResponseEntity<Void> refreshToken(HttpServletRequest request, HttpServletResponse response) {
        authService.refreshToken(request, response);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "토큰 무효화 및 로그아웃 처리")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        authService.logout(request, response);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    @Operation(summary = "내 정보 조회", description = "현재 로그인한 사용자 정보 조회")
    public ResponseEntity<UserResponseDTO> getMe(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomOAuth2User principal
    ) {
        UUID userId = principal.getUserId();
        UserResponseDTO user = userV2QueryService.getUserWithProfileImage(userId);
        return ResponseEntity.ok(user);
    }
}