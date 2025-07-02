package store.lastdance.dto.expense;

import io.swagger.v3.oas.annotations.media.Schema;
import store.lastdance.domain.expense.Expense;
import store.lastdance.domain.expense.ExpenseCategory;
import store.lastdance.domain.expense.ExpenseType;
import store.lastdance.domain.expense.SplitType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Schema(description = "지출 응답")
public record ExpenseResponseDTO(
        @Schema(description = "지출 ID")
        Long expenseId,

        @Schema(description = "지출 제목")
        String title,

        @Schema(description = "지출 금액")
        BigDecimal amount,

        @Schema(description = "지출 카테고리")
        ExpenseCategory category,

        @Schema(description = "지출 타입")
        ExpenseType expenseType,

        @Schema(description = "정산 방식")
        SplitType splitType,

        @Schema(description = "정산 데이터")
        List<SplitDataDTO> splitData,

        @Schema(description = "지출 날짜")
        LocalDate date,

        @Schema(description = "메모")
        String memo,

        @Schema(description = "그룹 ID")
        UUID groupId,

        @Schema(description = "사용자 ID")
        UUID userId,

        @Schema(description = "생성일시")
        LocalDateTime createdAt,

        @Schema(description = "수정일시")
        LocalDateTime updatedAt,

        @Schema(description = "영수증 파일 ID")
        UUID receiptImageFileId,

        @Schema(description = "영수증 존재 여부")
        boolean hasReceipt
) {

    public static ExpenseResponseDTO from(Expense expense) {
        return from(expense, null);
    }

    public static ExpenseResponseDTO from(Expense expense, List<SplitDataDTO> splitData) {
        return new ExpenseResponseDTO(
                expense.getExpenseId(),
                expense.getTitle(),
                expense.getAmount(),
                expense.getCategory(),
                expense.getExpenseType(),
                expense.getSplitType(),
                splitData,
                expense.getExpenseDate(),
                expense.getMemo(),
                expense.getGroupId(),
                expense.getUserId(),
                expense.getCreatedAt(),
                expense.getUpdatedAt(),
                expense.getReceiptImageFileId(),
                expense.getReceiptImageFileId() != null
        );
    }
}