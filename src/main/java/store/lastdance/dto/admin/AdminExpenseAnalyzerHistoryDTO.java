package store.lastdance.dto.admin;

import com.fasterxml.jackson.annotation.JsonFormat;
import store.lastdance.domain.expense.ExpenseAnalysisHistory;

import java.time.LocalDateTime;

public record AdminExpenseAnalyzerHistoryDTO(
        Long id,
        String email,
        String nickname,
        LocalDateTime createdAt,
        Boolean up,
        Boolean down
) {
    public static AdminExpenseAnalyzerHistoryDTO from(ExpenseAnalysisHistory history) {
        return new AdminExpenseAnalyzerHistoryDTO(
                history.getId(),
                history.getUser().getEmail(),
                history.getUser().getNickname(),
                history.getCreatedAt(),
                history.getUp(),
                history.getDown()
        );
    }
}
