package store.lastdance.dto.admin;

import java.time.LocalDateTime;
import java.util.UUID;

public record ReportManagementDTO(
        Long reportId,
        AdminPageUserDTO reporter,
        AdminPageUserDTO reportedUser,
        String reportType,
        UUID targetId,
        String reason,
        String status,
        LocalDateTime processedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
