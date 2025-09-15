package store.lastdance.dto.expense;

import java.math.BigDecimal;

public record SimpleExpenseStats(
        BigDecimal totalOriginalAmount,
        BigDecimal totalShareAmount,
        Long totalCount,
        BigDecimal maxShareAmount
) {
    public SimpleExpenseStats {
        totalOriginalAmount = (totalOriginalAmount != null) ? totalOriginalAmount : BigDecimal.ZERO;
        totalShareAmount = (totalShareAmount != null) ? totalShareAmount : BigDecimal.ZERO;
        totalCount = (totalCount != null) ? totalCount : 0L;
        maxShareAmount = (maxShareAmount != null) ? maxShareAmount : BigDecimal.ZERO;
    }
}
