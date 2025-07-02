package store.lastdance.dto.expense;

import io.swagger.v3.oas.annotations.media.Schema;
import store.lastdance.domain.expense.Expense;
import store.lastdance.domain.expense.ExpenseCategory;
import store.lastdance.domain.expense.SplitType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Schema(description = "그룹 공유 지출 응답")
public record GroupShareExpenseResponseDTO(
        @Schema(description = "지출 ID")
        Long expenseId,

        @Schema(description = "지출 제목")
        String title,

        @Schema(description = "총 지출 금액")
        BigDecimal amount,

        @Schema(description = "내가 부담할 금액")
        BigDecimal myShareAmount,

        @Schema(description = "지출 카테고리")
        ExpenseCategory category,

        @Schema(description = "지출 날짜")
        LocalDate date,

        @Schema(description = "메모")
        String memo,

        @Schema(description = "그룹 ID")
        UUID groupId,

        @Schema(description = "그룹 이름")
        String groupName,

        @Schema(description = "정산 방식")
        SplitType splitType,

        @Schema(description = "분할 상세 정보")
        List<SplitDataDTO> splitData,

        @Schema(description = "영수증 파일 ID")
        UUID receiptImageFileId,

        @Schema(description = "영수증 존재 여부")
        boolean hasReceipt,

        @Schema(description = "원본 지출 ID")
        Long originalExpenseId
) {

    public static GroupShareExpenseResponseDTO from(Expense shareExpense, Expense originalExpense, String groupName, List<SplitDataDTO> splitData) {
        return new GroupShareExpenseResponseDTO(
                shareExpense.getExpenseId(),
                shareExpense.getTitle(),
                originalExpense != null ? originalExpense.getAmount() : shareExpense.getAmount(), // 총 금액
                shareExpense.getAmount(), // 내 분담 금액
                shareExpense.getCategory(),
                shareExpense.getExpenseDate(),
                shareExpense.getMemo(),
                shareExpense.getGroupId(),
                groupName,
                originalExpense != null ? originalExpense.getSplitType() : shareExpense.getSplitType(),
                splitData,
                originalExpense.getReceiptImageFileId(),
                originalExpense.getReceiptImageFileId() != null,
                originalExpense.getExpenseId()
        );
    }
}