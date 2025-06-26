package store.lastdance.dto.admin;

import java.util.UUID;

public record UnbanResponseDTO(
        UUID userId,
        boolean isBanned,
        String banEndDate,
        String updatedAt
) {
}
