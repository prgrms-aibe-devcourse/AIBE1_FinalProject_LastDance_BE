package store.lastdance.dto.admin;

import java.time.LocalDateTime;

public record SignupTrendDTO(
        LocalDateTime date,
        long signups
) {
}
