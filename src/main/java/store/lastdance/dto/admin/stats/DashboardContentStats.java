package store.lastdance.dto.admin.stats;

public record DashboardContentStats(
        long totalPosts,
        long totalComments,
        long todayPosts,
        long todayComments
) {
}
