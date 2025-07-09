package store.lastdance.dto.admin;


import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record BanRequestDTO(

        @NotNull(message = "정지 종료일은 필수입니다.")
        @Future(message = "정지 종료일은 현재 시간 이후여야 합니다.")
        LocalDateTime banEndDate,
        String reason,
        boolean sendNotification
) {
}
