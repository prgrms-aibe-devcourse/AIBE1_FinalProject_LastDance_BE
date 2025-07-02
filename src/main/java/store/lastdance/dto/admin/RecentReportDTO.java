package store.lastdance.dto.admin;

import java.time.LocalDateTime;

public record RecentReportDTO(
        Long reportId,
        String reportType,
        String reason,
        String status,
        LocalDateTime createdAt
) {
}
