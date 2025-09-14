package store.lastdance.service.analysis;

import store.lastdance.domain.analysis.FeedbackType;

import java.util.UUID;

public interface AnalysisV2CommandService {
    FeedbackType toggleFeedback(Long historyId, UUID userId, FeedbackType type);
}
