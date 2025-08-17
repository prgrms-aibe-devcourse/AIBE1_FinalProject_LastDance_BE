package store.lastdance.dto.expense;

import io.swagger.v3.oas.annotations.media.Schema;

public record MonthlyTrendRequestDTO(
        @Schema(description = "조회 시작 연도")
        Integer year,
        @Schema(description = "조회 시작 월")
        Integer month,
        @Schema(description = "조회할 개월 수")
        Integer months,
        @Schema(description = "카테고리 (선택 사항)")
        String category
) {
}