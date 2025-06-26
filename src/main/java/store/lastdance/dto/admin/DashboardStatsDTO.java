package store.lastdance.dto.admin;

import store.lastdance.dto.admin.stats.DashboardContentStats;
import store.lastdance.dto.admin.stats.DashboardUserStats;

public record DashboardStatsDTO(
        DashboardUserStats dashboardUserStats,
        DashboardContentStats dashboardContentStats
) {
}
