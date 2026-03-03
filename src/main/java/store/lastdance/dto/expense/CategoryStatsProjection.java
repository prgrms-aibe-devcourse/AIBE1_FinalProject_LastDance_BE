package store.lastdance.dto.expense;

import store.lastdance.domain.expense.ExpenseCategory;

import java.math.BigDecimal;

public record CategoryStatsProjection(
        ExpenseCategory category,
        BigDecimal totalAmount,
        Long count
) {
}
