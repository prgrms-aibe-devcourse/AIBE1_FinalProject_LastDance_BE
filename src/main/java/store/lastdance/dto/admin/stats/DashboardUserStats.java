package store.lastdance.dto.admin.stats;

public record DashboardUserStats(
        long total,
        long active,
        long suspended,
        long newThisWeek
) {
}
