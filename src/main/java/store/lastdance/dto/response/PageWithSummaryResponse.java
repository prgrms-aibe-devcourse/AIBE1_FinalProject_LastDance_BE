package store.lastdance.dto.response;

import org.springframework.data.domain.Page;
import store.lastdance.dto.expense.ExpenseSummary;

public record PageWithSummaryResponse<T>(
        Page<T> page,
        ExpenseSummary summary
) {
    public static <T> PageWithSummaryResponse<T> of(Page<T> page, ExpenseSummary summary) {
        return new PageWithSummaryResponse<>(page, summary);
    }
}
