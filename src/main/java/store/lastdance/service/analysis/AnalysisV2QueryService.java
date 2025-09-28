package store.lastdance.service.analysis;

import org.springframework.data.domain.Pageable;
import store.lastdance.dto.analysis.AnalyzeExpenseRequestDTO;
import store.lastdance.dto.analysis.AnalyzeExpenseResponseDTO;
import store.lastdance.dto.analysis.ExpenseAnalysisHistoryDTO;
import store.lastdance.dto.response.PageWithSummaryResponse;

import java.util.UUID;

public interface AnalysisV2QueryService {
    AnalyzeExpenseResponseDTO analyzeExpenses(UUID userId, AnalyzeExpenseRequestDTO requestDTO);
    PageWithSummaryResponse<ExpenseAnalysisHistoryDTO> getExpenseAnalysisHistory(UUID userId, Pageable pageable);
}
