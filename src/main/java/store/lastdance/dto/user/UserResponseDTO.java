package store.lastdance.dto.user;

import store.lastdance.domain.user.User;

import java.util.UUID;

public record UserResponseDTO(
        UUID userId,
        String email,
        String username,
        String nickname,
        String profileImageUrl,
        String provider,
        boolean isActive,
        boolean isBanned,
        Integer monthlyBudget
) {
    public static UserResponseDTO from(User user) {
        String profileImageUrl = null;
        if (user.getProfileImageFile() != null) {
//            profileImageUrl = user.getProfileImageFile().getFilePath();
            profileImageUrl = "/api/v1/images/" + user.getProfileImageFile().getFileId();
        }

        return new UserResponseDTO(
                user.getUserId(),
                user.getEmail(),
                user.getUsername(),
                user.getNickname(),
                profileImageUrl,
                user.getProvider().name(),
                user.getIsActive(),
                user.getIsBanned(),
                user.getUserBudget()
        );
    }
}