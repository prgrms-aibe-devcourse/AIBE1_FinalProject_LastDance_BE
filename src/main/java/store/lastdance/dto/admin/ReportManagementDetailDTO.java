package store.lastdance.dto.admin;

import java.time.LocalDateTime;

public record ReportManagementDetailDTO(
        long reportId,
        UserManagementDTO reporter,
        UserManagementDTO reportedUser,
        String reportType,
        String targetId,
        TargetContentDTO targetContent,
        String reason,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
