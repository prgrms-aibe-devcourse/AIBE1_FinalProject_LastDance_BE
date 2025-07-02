package store.lastdance.dto.admin;

import store.lastdance.dto.admin.stats.UserStats;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record UserManagementDetailDTO(
        UUID userId,
        String email,
        String username,
        String nickname,
        String provider,
        String providerId,
        String role,
        boolean isActive,
        boolean isBanned,
        LocalDateTime banEndDate,
        long userBudget,
        UUID profileImageFileId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime inactivedAt,
        UserStats stats,
        List<RecentReportDTO> recentReports
) {
}
