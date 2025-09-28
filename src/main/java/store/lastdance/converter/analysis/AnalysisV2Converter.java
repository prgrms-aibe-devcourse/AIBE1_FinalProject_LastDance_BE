package store.lastdance.converter.analysis;

import org.springframework.stereotype.Component;
import store.lastdance.domain.analysis.ExpenseAnalysisHistory;
import store.lastdance.dto.analysis.AnalyzeExpenseResponseDTO;
import store.lastdance.dto.analysis.ExpenseAnalysisHistoryDTO;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Component
public class AnalysisV2Converter {

    public ExpenseAnalysisHistoryDTO toExpenseAnalysisHistoryDTO(ExpenseAnalysisHistory history) {
        return ExpenseAnalysisHistoryDTO.from(history);
    }

    public AnalyzeExpenseResponseDTO toAnalyzeExpenseResponseDTO(
            Long historyId,
            AnalyzeExpenseResponseDTO.BudgetUsage budgetUsage,
            AnalyzeExpenseResponseDTO.DailySpending dailySpending,
            AnalyzeExpenseResponseDTO.AnalysisResult analysisResult,
            List<AnalyzeExpenseResponseDTO.CategoryDetail> categoryDetails
    ) {
        return new AnalyzeExpenseResponseDTO(historyId, budgetUsage, dailySpending, analysisResult, categoryDetails == null ? List.of() : List.copyOf(categoryDetails));
    }

    public AnalyzeExpenseResponseDTO.BudgetUsage toBudgetUsage(double percentage, BigDecimal currentSpending, BigDecimal totalBudget) {
        BigDecimal roundedPercentage = BigDecimal.valueOf(percentage).setScale(2, RoundingMode.HALF_UP);
        return new AnalyzeExpenseResponseDTO.BudgetUsage(roundedPercentage.doubleValue(), currentSpending, totalBudget);
    }

    public AnalyzeExpenseResponseDTO.DailySpending toDailySpending(BigDecimal averageSoFar, BigDecimal estimatedEom) {
        return new AnalyzeExpenseResponseDTO.DailySpending(averageSoFar, estimatedEom);
    }

    public AnalyzeExpenseResponseDTO.AnalysisResult toAnalysisResult(String mainFinding, AnalyzeExpenseResponseDTO.Suggestion suggestion) {
        return new AnalyzeExpenseResponseDTO.AnalysisResult(mainFinding, suggestion);
    }

    public AnalyzeExpenseResponseDTO.CategoryDetail toCategoryDetail(String category, double percentage, BigDecimal totalAmount, int transactionCount) {
        BigDecimal roundedPercentage = BigDecimal.valueOf(percentage).setScale(2, RoundingMode.HALF_UP);
        return new AnalyzeExpenseResponseDTO.CategoryDetail(category, roundedPercentage.doubleValue(), totalAmount, transactionCount);
    }
}
