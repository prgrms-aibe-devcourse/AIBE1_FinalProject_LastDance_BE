package store.lastdance.dto.admin;

import java.time.LocalDateTime;

public record AiJudgmentTrendsDTO(
        LocalDateTime date,
        double satisfactionRate
) {
}
