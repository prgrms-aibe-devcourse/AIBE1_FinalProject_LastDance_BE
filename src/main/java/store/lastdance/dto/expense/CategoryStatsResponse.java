package store.lastdance.dto.expense;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "카테고리별 통계")
public record CategoryStatsResponse(
        @Schema(description = "카테고리별 총 금액")
        BigDecimal amount,

        @Schema(description = "카테고리별 건수")
        Long count,

        @Schema(description = "전체 대비 비율 (%)")
        BigDecimal percentage
) {
}