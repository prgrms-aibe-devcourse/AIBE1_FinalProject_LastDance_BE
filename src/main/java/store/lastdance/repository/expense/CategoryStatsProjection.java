package store.lastdance.repository.expense;

import store.lastdance.domain.expense.ExpenseCategory;

import java.math.BigDecimal;

public interface CategoryStatsProjection {
    ExpenseCategory getCategory();
    BigDecimal getTotalAmount();
    Long getCount();
}
