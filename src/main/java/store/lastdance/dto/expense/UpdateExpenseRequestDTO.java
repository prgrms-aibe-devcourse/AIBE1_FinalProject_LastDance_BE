package store.lastdance.dto.expense;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import store.lastdance.domain.expense.ExpenseCategory;
import store.lastdance.domain.expense.SplitType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "지출 수정 요청")
public record UpdateExpenseRequestDTO(
        @Schema(description = "지출 제목", example = "저녁식사")
        @NotBlank(message = "제목은 필수입니다")
        String title,

        @Schema(description = "지출 금액", example = "25000")
        @NotNull(message = "금액은 필수입니다")
        @Positive(message = "금액은 0보다 커야 합니다")
        BigDecimal amount,

        @Schema(description = "지출 카테고리", example = "FOOD")
        @NotNull(message = "카테고리는 필수입니다")
        ExpenseCategory category,

        @Schema(description = "지출 날짜", example = "2025-01-15")
        @NotNull(message = "날짜는 필수입니다")
        LocalDate date,

        @Schema(description = "메모", example = "회식비")
        String memo,

        @Schema(description = "커스텀 분할 데이터 (그룹지출, CUSTOM/SPECIFIC 분할의 경우)")
        List<SplitDataDTO> splitData,

        @Schema(description = "분할 타입 (그룹지출)")
        SplitType splitType
) {
}