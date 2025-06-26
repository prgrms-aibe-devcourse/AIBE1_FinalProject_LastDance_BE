package store.lastdance.dto.admin;

public record ReportProcessResponseDTO(
        Long reportId,
        String status,
        String adminId,
        String processedAt,
        ReportActionDTO[] actions
) {
}
