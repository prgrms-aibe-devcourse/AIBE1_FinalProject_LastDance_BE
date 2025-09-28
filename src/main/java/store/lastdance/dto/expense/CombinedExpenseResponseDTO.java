package store.lastdance.dto.expense;

import io.swagger.v3.oas.annotations.media.Schema;
import store.lastdance.domain.expense.ExpenseCategory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "통합 지출 응답")
public record CombinedExpenseResponseDTO(
        @Schema(description = "지출 ID")
        Long expenseId,

        @Schema(description = "원본 지출 ID")
        Long originalExpenseId,

        @Schema(description = "지출 유형", example = "PERSONAL 또는 GROUP_SHARE")
        String expenseType,

        @Schema(description = "지출 제목")
        String title,

        @Schema(description = "총 지출 금액")
        BigDecimal amount,

        @Schema(description = "내 분담 금액")
        BigDecimal myShareAmount,

        @Schema(description = "지출 카테고리")
        ExpenseCategory category,

        @Schema(description = "지출 날짜")
        LocalDateTime date,

        @Schema(description = "메모")
        String memo,

        @Schema(description = "영수증 존재 여부")
        Boolean hasReceipt,

        @Schema(description = "그룹 ID")
        UUID groupId,

        @Schema(description = "그룹 이름")
        String groupName
) {
}