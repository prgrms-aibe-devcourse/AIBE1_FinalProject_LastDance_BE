package store.lastdance.dto.expense;

import java.math.BigDecimal;

public record SimpleExpenseStats(
        BigDecimal totalOriginalAmount,
        BigDecimal totalShareAmount,
        Long totalCount,
        Double averageShareAmount,
        BigDecimal maxShareAmount
) {
    public SimpleExpenseStats {
        totalOriginalAmount = (totalOriginalAmount != null) ? totalOriginalAmount : BigDecimal.ZERO;
        totalShareAmount = (totalShareAmount != null) ? totalShareAmount : BigDecimal.ZERO;
        totalCount = (totalCount != null) ? totalCount : 0L;
        averageShareAmount = (averageShareAmount != null) ? averageShareAmount : 0.0;
        maxShareAmount = (maxShareAmount != null) ? maxShareAmount : BigDecimal.ZERO;
    }
}
