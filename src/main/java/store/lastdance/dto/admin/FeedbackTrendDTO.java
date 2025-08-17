package store.lastdance.dto.admin;

public record FeedbackTrendDTO(
    int year,
    int month,
    int day,
    Long totalCount,
    Long upCount,
    Long downCount
) {}
