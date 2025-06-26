package store.lastdance.dto.admin;

import java.util.UUID;

public record AdminPageUserDTO(
        UUID userId,
        String email,
        String nickname
) {
}
