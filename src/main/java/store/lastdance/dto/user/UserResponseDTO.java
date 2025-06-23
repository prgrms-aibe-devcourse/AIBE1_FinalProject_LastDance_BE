package store.lastdance.dto.user;

import store.lastdance.domain.user.User;

import java.util.UUID;

public record UserResponseDTO(
        UUID userId,
        String email,
        String username,
        String nickname,
//        String profileImageUrl,
        String provider,
        boolean isActive,
        boolean isBanned
) {
    public static UserResponseDTO from(User user) {
        return new UserResponseDTO(
                user.getUserId(), user.getEmail(), user.getUsername(), user.getNickname(),
                user.getProvider().name(), user.getIsActive(), user.getIsBanned()
        );
    }
}