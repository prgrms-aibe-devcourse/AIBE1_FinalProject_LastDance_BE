package store.lastdance.dto.admin;


import java.time.LocalDateTime;

public record BanRequestDTO(
        LocalDateTime banEndDate,
        String reason,
        boolean sendNotification
) {
}
