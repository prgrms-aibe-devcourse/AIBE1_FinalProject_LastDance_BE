package store.lastdance.dto.admin;

import store.lastdance.dto.admin.stats.UserStats;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserManagementDTO(
        UUID userId,
        String email,
        String username,
        String nickname,
        String provider,
        String role,
        boolean isActive,
        boolean isBanned,
        LocalDateTime banEndDate,
        long userBudget,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        UserStats stats
) {
}
