package store.lastdance.dto.admin;

public record ReportProcessRequestDTO(
        String status,
        boolean banUser,
        String banEndDate,
        boolean sendNotification
) {
}
