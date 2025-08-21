package store.lastdance.dto.analysis;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.Objects;

@Schema(description = "LLM 분석 요청")
public record AnalyzeExpenseRequestDTO(
        @NotNull(message = "시작일은 필수입니다.")
        LocalDate startDate,
        @NotNull(message = "종료일은 필수입니다.")
        LocalDate endDate
) {
    public AnalyzeExpenseRequestDTO {
        Objects.requireNonNull(startDate, "시작일은 null이 될 수 없습니다.");
        Objects.requireNonNull(endDate, "종료일은 null이 될 수 없습니다.");
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("시작일은 종료일보다 늦을 수 없습니다.");
        }
    }
}