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
import java.util.UUID;

@Schema(description = "그룹 지출 생성 요청")
public record CreateGroupExpenseRequestDTO(
        @Schema(description = "지출 제목", example = "회식비")
        @NotBlank(message = "제목은 필수입니다")
        String title,

        @Schema(description = "지출 금액", example = "50000")
        @NotNull(message = "금액은 필수입니다")
        @Positive(message = "금액은 0보다 커야 합니다")
        BigDecimal amount,

        @Schema(description = "지출 카테고리", example = "FOOD")
        @NotNull(message = "카테고리는 필수입니다")
        ExpenseCategory category,

        @Schema(description = "지출 날짜", example = "2025-07-02")
        @NotNull(message = "날짜는 필수입니다")
        LocalDate date,

        @Schema(description = "메모", example = "팀 회식")
        String memo,

        @Schema(description = "그룹 ID", example = "550e8400-e29b-41d4-a716-446655440000")
        @NotNull(message = "그룹 ID는 필수입니다")
        UUID groupId,

        @Schema(description = "정산 방식", example = "EQUAL")
        @NotNull(message = "정산 방식은 필수입니다")
        SplitType splitType,

        @Schema(description = "정산 데이터 (custom/specific일 때)")
        List<SplitDataDTO> splitData

) implements BaseExpenseRequest {
}
