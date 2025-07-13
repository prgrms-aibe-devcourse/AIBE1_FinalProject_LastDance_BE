package store.lastdance.dto.admin;

import java.time.LocalDateTime;


import java.util.List;

public record ExpenseAnalyzerFeedbackStatsDTO(
    long totalFeedbacks,
    long upCount,
    long downCount,
    double satisfactionRate,
    List<FeedbackTrendDTO> trends
) {
    public record FeedbackTrendDTO(
        String date,
        long totalCount,
        long upCount,
        long downCount
    ) {}
}
