package store.lastdance.dto.admin;

public record ReportActionDTO(
        String type,
        String userId,
        String banEndDate
) {
}
