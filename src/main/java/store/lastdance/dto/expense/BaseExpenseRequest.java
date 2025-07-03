package store.lastdance.dto.expense;

import store.lastdance.domain.expense.ExpenseCategory;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface BaseExpenseRequest {
    String title();
    BigDecimal amount();
    ExpenseCategory category();
    LocalDate date();
    String memo();
}
