package store.lastdance.dto.expense;

import store.lastdance.dto.calender.DateRangeDTO;

import java.time.LocalDate;
import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;

public record MonthlyExpenseTrendResponseDTO(
        @Schema(description = "월별 지출 데이터 (yyyy-MM -> 해당 월의 지출 목록)")
        Map<String, List<ExpenseResponseDTO>> monthlyData,
        @Schema(description = "총 지출 건수")
        Integer totalCount,
        @Schema(description = "조회 기간")
        DateRangeDTO dateRange
) {
    public static MonthlyExpenseTrendResponseDTO create(Map<String, List<ExpenseResponseDTO>> monthlyData, LocalDate startDate, LocalDate endDate) {
        DateRangeDTO dateRangeDTO = new DateRangeDTO(
                startDate.atStartOfDay(),
                endDate.atTime(23, 59, 59)
        );

        int totalCount = monthlyData.values().stream()
                .mapToInt(List::size)
                .sum();

        return new MonthlyExpenseTrendResponseDTO(monthlyData, totalCount, dateRangeDTO);
    }
}