package store.lastdance.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import store.lastdance.domain.user.User;

import java.util.UUID;

@Schema(description = "사용자 정보 응답")
public record UserResponseDTO(
        @Schema(description = "사용자 ID", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID userId,

        @Schema(description = "이메일", example = "user@example.com")
        String email,

        @Schema(description = "사용자명", example = "testuser")
        String username,

        @Schema(description = "닉네임", example = "멋진사용자")
        String nickname,

        @Schema(description = "프로필 이미지 URL", example = "https://example.com/profile.jpg")
        String profileImageUrl,

        @Schema(description = "소셜 로그인 제공자", example = "GOOGLE")
        String provider,

        @Schema(description = "활성 상태", example = "true")
        Boolean isActive,

        @Schema(description = "밴 상태", example = "false")
        Boolean isBanned,

        @Schema(description = "월 예산", example = "1000000")
        Integer monthlyBudget
) {
    public static UserResponseDTO from(User user) {
        String profileImageUrl = null;
        if (user.getProfileImageFile() != null) {
            profileImageUrl = user.getProfileImageFile().getFilePath();
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