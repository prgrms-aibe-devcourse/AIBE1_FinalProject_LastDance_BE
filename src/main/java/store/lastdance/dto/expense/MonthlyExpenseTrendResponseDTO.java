package store.lastdance.dto.expense;

import store.lastdance.dto.calendar.DateRangeDTO;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

public record MonthlyExpenseTrendResponseDTO(
        @Schema(description = "월별 지출 데이터 (yyyy-MM -> 해당 월의 지출 목록)")
        Map<String, List<ExpenseResponseDTO>> monthlyData,
        @Schema(description = "총 지출 건수")
        Integer totalCount,
        @Schema(description = "조회 기간")
        DateRangeDTO dateRange
) {
}