package store.lastdance.dto.admin;

import java.time.LocalDateTime;
import java.util.UUID;

public record BanResponseDTO(
        UUID userId,
        boolean isBanned,
        LocalDateTime banEndDate,
        LocalDateTime updatedAt
) {
}
