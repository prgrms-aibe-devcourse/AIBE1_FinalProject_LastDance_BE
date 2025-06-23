package store.lastdance.dto.group;

import java.time.LocalDateTime;
import java.util.UUID;

public record GroupApplicationResponseDTO(
        UUID userId,
        UUID groupId,
        String nickname,
        String email,
        String profileImagePath,
        LocalDateTime updatedAt
) {
}
