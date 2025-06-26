package store.lastdance.dto.admin;

public record UnbanRequestDTO(
        String reason,
        boolean sendNotification
) {
}
