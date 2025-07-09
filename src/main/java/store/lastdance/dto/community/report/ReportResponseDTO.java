package store.lastdance.dto.community.report;

import store.lastdance.domain.admin.ReportStatus;
import store.lastdance.domain.admin.ReportType;
import java.time.LocalDateTime;
import java.util.UUID;

public record ReportResponseDTO(
        Long reportId,
        UUID reporterId,
        UUID reportedUserId,
        ReportType reportType,
        UUID targetId,
        String reason,
        ReportStatus status,
        UUID adminId,
        String adminComment,
        LocalDateTime processedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}