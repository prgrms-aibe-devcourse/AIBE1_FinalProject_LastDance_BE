package store.lastdance.dto.group;

import java.time.LocalDateTime;

public record GroupApplicationResponseDTO(
        String nickname,
        String email,
        String profileImagePath,
        LocalDateTime updatedAt
) {
}
