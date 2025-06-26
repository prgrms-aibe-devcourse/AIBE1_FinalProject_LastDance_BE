package store.lastdance.dto.admin;

import store.lastdance.domain.user.UserRole;

import java.util.UUID;

public record AdminVerifyResponseDTO(
        UUID userId,
        String email,
        String nickname,
        UserRole role) {
}
