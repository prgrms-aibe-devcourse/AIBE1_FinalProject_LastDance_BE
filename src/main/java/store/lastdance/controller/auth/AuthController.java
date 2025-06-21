package store.lastdance.controller.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import store.lastdance.domain.user.User;
import store.lastdance.dto.user.UserResponseDTO;
import store.lastdance.security.CurrentUser;
import store.lastdance.security.oauth.CustomOAuth2User;
import store.lastdance.service.auth.AuthService;
import store.lastdance.service.user.UserService;

import java.util.UUID;

// 인증 관련 API 컨트롤러
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    @PostMapping("/refresh")
    public ResponseEntity<Void> refreshToken(HttpServletRequest request, HttpServletResponse response) {
        authService.refreshToken(request, response);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        authService.logout(request, response);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponseDTO> getMe(@CurrentUser CustomOAuth2User principal) {
        UUID userId = principal.getUserId();
        User user = userService.findByUserId(userId);

        UserResponseDTO dto = UserResponseDTO.from(user);
        return ResponseEntity.ok(dto);
    }

}
