package store.lastdance.dto.community.report;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import store.lastdance.domain.admin.ReportType;
import java.util.UUID;

public record ReportRequestDTO(
        @NotNull(message = "신고 타입은 필수입니다.")
        ReportType reportType,

        @NotNull(message = "신고 대상 ID는 필수입니다.")
        UUID targetId,

        @NotBlank(message = "신고 사유는 필수입니다.")
        @Size(max = 300, message = "신고 사유는 300자를 초과할 수 없습니다.")
        String reason
) {
}