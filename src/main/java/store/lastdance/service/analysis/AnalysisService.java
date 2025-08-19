package store.lastdance.service.analysis;

import org.springframework.data.domain.Pageable;
import store.lastdance.dto.analysis.AnalyzeExpenseRequestDTO;
import store.lastdance.dto.analysis.AnalyzeExpenseResponseDTO;
import store.lastdance.dto.analysis.ExpenseAnalysisHistoryDTO;
import store.lastdance.dto.response.PageWithSummaryResponse;

import java.util.UUID;

public interface AnalysisService {
    AnalyzeExpenseResponseDTO analyzeExpenses(UUID userId, AnalyzeExpenseRequestDTO requestDTO);
    String toggleFeedback(Long historyId, UUID userId, String type);
    PageWithSummaryResponse<ExpenseAnalysisHistoryDTO> getExpenseAnalysisHistory(UUID userId, Pageable pageable);
}