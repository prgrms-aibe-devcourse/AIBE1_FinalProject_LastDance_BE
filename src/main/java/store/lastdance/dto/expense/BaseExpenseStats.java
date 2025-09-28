package store.lastdance.dto.expense;

import java.math.BigDecimal;

public record BaseExpenseStats(
        BigDecimal totalAmount,
        Long totalCount,
        BigDecimal maxAmount
) {
    public BaseExpenseStats {
        totalAmount = (totalAmount != null) ? totalAmount : BigDecimal.ZERO;
        totalCount = (totalCount != null) ? totalCount : 0L;
        maxAmount = (maxAmount != null) ? maxAmount : BigDecimal.ZERO;
    }
}
