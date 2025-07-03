package store.lastdance.dto.expense;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import store.lastdance.domain.expense.ExpenseCategory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Schema(description = "개인 지출 생성 요청")
public record CreatePersonalExpenseRequestDTO(
        @Schema(description = "지출 제목", example = "점심식사")
        @NotBlank(message = "제목은 필수입니다")
        String title,

        @Schema(description = "지출 금액", example = "15000")
        @NotNull(message = "금액은 필수입니다")
        @Positive(message = "금액은 0보다 커야 합니다")
        BigDecimal amount,

        @Schema(description = "지출 카테고리", example = "FOOD")
        @NotNull(message = "카테고리는 필수입니다")
        ExpenseCategory category,

        @Schema(description = "지출 날짜", example = "2025-07-02")
        @NotNull(message = "날짜는 필수입니다")
        LocalDate date,

        @Schema(description = "메모", example = "치킨집에서 점심")
        String memo

) implements BaseExpenseRequest {
}
