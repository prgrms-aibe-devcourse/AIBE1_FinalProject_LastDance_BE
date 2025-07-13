package store.lastdance.dto.admin;

import java.util.List;

public record ExpenseAnalyzerFeedbackStatsDTO(
    long totalFeedbacks,
    Long upCount,
    Long downCount,
    double satisfactionRate,
    List<FeedbackTrendDTO> trends
) {
}

