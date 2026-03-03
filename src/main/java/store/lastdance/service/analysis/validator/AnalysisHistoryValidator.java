package store.lastdance.service.analysis.validator;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import store.lastdance.domain.analysis.ExpenseAnalysisHistory;
import store.lastdance.exception.CustomException;
import store.lastdance.exception.ErrorCode;
import store.lastdance.repository.analysis.ExpenseAnalysisHistoryRepository;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AnalysisHistoryValidator {

    private final ExpenseAnalysisHistoryRepository expenseAnalysisHistoryRepository;

    public ExpenseAnalysisHistory validate(Long historyId, UUID userId) {
        // 1. 분석 내역 존재 여부 검증
        ExpenseAnalysisHistory history = expenseAnalysisHistoryRepository.findById(historyId)
                .orElseThrow(() -> new CustomException(ErrorCode.HISTORY_NOT_FOUND));

        // 2. 사용자 권한 검증
        if (!history.getUser().getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.EXPENSE_ACCESS_DENIED);
        }

        return history;
    }
}
