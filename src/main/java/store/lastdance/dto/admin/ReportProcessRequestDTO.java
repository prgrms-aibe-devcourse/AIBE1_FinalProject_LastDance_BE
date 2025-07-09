package store.lastdance.dto.admin;

import jakarta.validation.constraints.NotNull;
import store.lastdance.domain.admin.ReportStatus;
import store.lastdance.validation.admin.ValidBanEndDate;

import java.time.LocalDateTime;

@ValidBanEndDate
public record ReportProcessRequestDTO(

        @NotNull(message = "신고 처리 상태는 필수입니다.")
        ReportStatus status,
        boolean banUser,
        LocalDateTime banEndDate,
        boolean sendNotification
) {
}
