package store.lastdance.dto.expense;

import io.swagger.v3.oas.annotations.media.Schema;
import store.lastdance.domain.expense.ExpenseCategory;
import store.lastdance.domain.expense.SplitType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Schema(description = "그룹 통합 지출 응답")
public record GroupCombinedExpenseResponseDTO(
        @Schema(description = "지출 ID")
        Long expenseId,

        @Schema(description = "지출 유형", example = "GROUP_EXPENSE")
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
        LocalDate date,

        @Schema(description = "메모")
        String memo,

        @Schema(description = "영수증 존재 여부")
        Boolean hasReceipt,

        @Schema(description = "그룹 ID")
        UUID groupId,

        @Schema(description = "그룹 이름")
        String groupName,

        @Schema(description = "지출 생성자 ID")
        UUID createdBy,

        @Schema(description = "지출 생성자 이름")
        String createdByName,

        @Schema(description = "분담 방식")
        SplitType splitType,

        @Schema(description = "참여자 목록")
        List<ParticipantDTO> participants
) {
    @Schema(description = "참여자 정보")
    public record ParticipantDTO(
            @Schema(description = "참여자 ID")
            UUID userId,

            @Schema(description = "참여자 이름")
            String userName,

            @Schema(description = "분담 금액")
            BigDecimal shareAmount
    ) {
    }
}
