package store.lastdance.dto.admin.stats;

public record UserStats(
        long postCount,
        long commentCount,
        long groupCount,
        long reportCount
) {
}
