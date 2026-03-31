package store.lastdance.dto.expense;

import store.lastdance.domain.expense.ExpenseCategory;

import java.math.BigDecimal;

public record ShareCategoryStatsProjection(
        ExpenseCategory category,
        BigDecimal totalShareAmount,
        BigDecimal totalOriginalAmount,
        Long count
) implements CategoryStats {
    @Override
    public BigDecimal totalAmount() {
        return totalShareAmount;
    }
}
