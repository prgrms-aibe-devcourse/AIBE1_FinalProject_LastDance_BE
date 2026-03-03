package store.lastdance.dto.expense;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.Map;

@Schema(description = "지출 통계 정보")
public record ExpenseSummary(
        @Schema(description = "총 지출 금액")
        BigDecimal totalAmount,

        @Schema(description = "평균 지출 금액")
        BigDecimal averageAmount,

        @Schema(description = "최대 지출 금액")
        BigDecimal maxAmount,

        @Schema(description = "총 지출 건수")
        Long totalCount,

        @Schema(description = "내 총 분담금")
        BigDecimal myTotalShareAmount,

        @Schema(description = "내 분담금 건수")
        Long myShareCount,

        @Schema(description = "카테고리별 통계")
        Map<String, CategoryStatsResponse> categoryStats,

        // 최대 지출 정보 추가
        @Schema(description = "최대 지출 ID")
        Long maxExpenseId,

        @Schema(description = "최대 지출 제목")
        String maxExpenseTitle
) {
    public static ExpenseSummary empty() {
        return new ExpenseSummary(
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                0L,
                BigDecimal.ZERO,
                0L,
                Map.of(),
                null,
                null
        );
    }
}