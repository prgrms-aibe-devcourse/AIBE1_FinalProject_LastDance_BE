package store.lastdance.service.analysis;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import store.lastdance.domain.analysis.ExpenseAnalysisHistory;
import store.lastdance.domain.analysis.FeedbackType;
import store.lastdance.exception.CustomException;
import store.lastdance.exception.ErrorCode;
import store.lastdance.repository.analysis.ExpenseAnalysisHistoryRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AnalysisV2CommandServiceImpl implements AnalysisV2CommandService {

    private final ExpenseAnalysisHistoryRepository expenseAnalysisHistoryRepository;

    @Override
    public FeedbackType toggleFeedback(Long historyId, UUID userid, FeedbackType type) {
        ExpenseAnalysisHistory history = expenseAnalysisHistoryRepository.findById(historyId)
                .orElseThrow(() -> new CustomException(ErrorCode.HISTORY_NOT_FOUND));

        if (!history.getUser().getUserId().equals(userid)) {
            throw new CustomException(ErrorCode.EXPENSE_ACCESS_DENIED);
        }

        boolean isUp = (type == FeedbackType.UP);
        boolean isDown = (type == FeedbackType.DOWN);

        // 현재 상태와 같은 버튼을 다시 누르면 피드백 취소
        if ((isUp && Boolean.TRUE.equals(history.getUp())) || (isDown && Boolean.TRUE.equals(history.getDown()))) {
            history.feedback(null, null);
            return null;
        } else {
            // 새로운 피드백 설정
            history.feedback(isUp, isDown);
            return type;
        }
    }
}
