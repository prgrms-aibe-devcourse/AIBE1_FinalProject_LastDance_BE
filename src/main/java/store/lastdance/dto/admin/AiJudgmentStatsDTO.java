package store.lastdance.dto.admin;

import java.util.List;

public record AiJudgmentStatsDTO(
        double satisfactionRate,
        int dissatisfactionCount,
        List<AiJudgmentTrendsDTO> trends
) {
}
