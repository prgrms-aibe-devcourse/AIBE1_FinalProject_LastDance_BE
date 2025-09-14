package store.lastdance.service.analysis;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import store.lastdance.domain.analysis.ExpenseAnalysisHistory;
import store.lastdance.domain.analysis.FeedbackType;
import store.lastdance.service.analysis.validator.AnalysisHistoryValidator;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AnalysisV2CommandServiceImpl implements AnalysisV2CommandService {

    private final AnalysisHistoryValidator analysisHistoryValidator;

    @Override
    public FeedbackType toggleFeedback(Long historyId, UUID userid, FeedbackType type) {
        // 검증 로직 위임
        ExpenseAnalysisHistory history = analysisHistoryValidator.validate(historyId, userid);

        // 비즈니스 로직
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
