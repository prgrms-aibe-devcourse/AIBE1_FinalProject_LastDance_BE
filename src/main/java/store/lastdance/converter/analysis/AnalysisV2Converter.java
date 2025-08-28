package store.lastdance.converter.analysis;

import org.springframework.stereotype.Component;
import store.lastdance.domain.analysis.ExpenseAnalysisHistory;
import store.lastdance.dto.analysis.AnalyzeExpenseResponseDTO;
import store.lastdance.dto.analysis.ExpenseAnalysisHistoryDTO;

import java.math.BigDecimal;
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
        return new AnalyzeExpenseResponseDTO(historyId, budgetUsage, dailySpending, analysisResult, categoryDetails);
    }

    public AnalyzeExpenseResponseDTO.BudgetUsage toBudgetUsage(double percentage, BigDecimal currentSpending, BigDecimal totalBudget) {
        return new AnalyzeExpenseResponseDTO.BudgetUsage(percentage, currentSpending, totalBudget);
    }

    public AnalyzeExpenseResponseDTO.DailySpending toDailySpending(BigDecimal averageSoFar, BigDecimal estimatedEom) {
        return new AnalyzeExpenseResponseDTO.DailySpending(averageSoFar, estimatedEom);
    }

    public AnalyzeExpenseResponseDTO.AnalysisResult toAnalysisResult(String mainFinding, AnalyzeExpenseResponseDTO.Suggestion suggestion) {
        return new AnalyzeExpenseResponseDTO.AnalysisResult(mainFinding, suggestion);
    }

    public AnalyzeExpenseResponseDTO.CategoryDetail toCategoryDetail(String category, double percentage, BigDecimal totalAmount, int transactionCount) {
        return new AnalyzeExpenseResponseDTO.CategoryDetail(category, percentage, totalAmount, transactionCount);
    }
}
