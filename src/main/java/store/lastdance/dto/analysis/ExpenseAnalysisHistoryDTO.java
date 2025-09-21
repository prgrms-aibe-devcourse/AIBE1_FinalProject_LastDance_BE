package store.lastdance.dto.analysis;

import io.swagger.v3.oas.annotations.media.Schema;
import store.lastdance.domain.analysis.ExpenseAnalysisHistory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "지출 분석 내역 응답 DTO")
public record ExpenseAnalysisHistoryDTO(
    @Schema(description = "내역 ID")
    Long id,

    @Schema(description = "분석 시작일")
    LocalDate startDate,

    @Schema(description = "분석 종료일")
    LocalDate endDate,

    @Schema(description = "예산 사용률 (%)")
    Double budgetUsagePercentage,

    @Schema(description = "현재 사용액")
    BigDecimal budgetUsageCurrentSpending,

    @Schema(description = "총 예산")
    BigDecimal budgetUsageTotalBudget,

    @Schema(description = "일평균 지출액")
    BigDecimal dailySpendingAverageSoFar,

    @Schema(description = "월말 예상 지출액")
    BigDecimal dailySpendingEstimatedEom,

    @Schema(description = "핵심 소비 패턴 요약")
    String mainFinding,

    @Schema(description = "개선 제안 제목")
    String suggestionTitle,

    @Schema(description = "개선 제안 내용")
    String suggestionDescription,

    @Schema(description = "개선 제안 예상 효과")
    String suggestionEffect,

    @Schema(description = "개선 제안 난이도")
    String suggestionDifficulty,

    @Schema(description = "분석 생성일")
    LocalDateTime createdAt
) {
    public static ExpenseAnalysisHistoryDTO from(ExpenseAnalysisHistory history) {
        return new ExpenseAnalysisHistoryDTO(
            history.getId(),
            history.getStartDate(),
            history.getEndDate(),
            history.getBudgetUsagePercentage(),
            history.getBudgetUsageCurrentSpending(),
            history.getBudgetUsageTotalBudget(),
            history.getDailySpendingAverageSoFar(),
            history.getDailySpendingEstimatedEom(),
            history.getMainFinding(),
            history.getSuggestionTitle(),
            history.getSuggestionDescription(),
            history.getSuggestionEffect(),
            history.getSuggestionDifficulty(),
            history.getCreatedAt()
        );
    }
}
