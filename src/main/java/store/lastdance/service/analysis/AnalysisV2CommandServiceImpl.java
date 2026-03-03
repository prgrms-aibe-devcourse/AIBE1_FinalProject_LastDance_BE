package store.lastdance.service.analysis;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import store.lastdance.domain.analysis.ExpenseAnalysisHistory;
import store.lastdance.domain.analysis.FeedbackType;
import store.lastdance.exception.CustomException;
import store.lastdance.exception.ErrorCode;
import store.lastdance.repository.analysis.ExpenseAnalysisHistoryRepository;
import store.lastdance.service.analysis.validator.AnalysisHistoryValidator;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AnalysisV2CommandServiceImpl implements AnalysisV2CommandService {

    private final AnalysisHistoryValidator analysisHistoryValidator;
    private final ExpenseAnalysisHistoryRepository expenseAnalysisHistoryRepository;

    @Override
    public FeedbackType toggleFeedback(Long historyId, UUID userId, FeedbackType type) {
        // 검증 로직 위임
        ExpenseAnalysisHistory history = analysisHistoryValidator.validate(historyId, userId);

        try {
            // 비즈니스 로직
            boolean isUp = (type == FeedbackType.UP);
            boolean isDown = (type == FeedbackType.DOWN);

            boolean cancel = (isUp && Boolean.TRUE.equals(history.getUp())) || (isDown && Boolean.TRUE.equals(history.getDown()));
            if (cancel) {
                history.feedback(null, null);
            } else {
                history.feedback(isUp, isDown);
            }

            expenseAnalysisHistoryRepository.saveAndFlush(history);
            return cancel ? null : type;

        } catch (OptimisticLockingFailureException e) {
            throw new CustomException(ErrorCode.OPTIMISTIC_LOCK_FAILURE);
        }
    }
}
