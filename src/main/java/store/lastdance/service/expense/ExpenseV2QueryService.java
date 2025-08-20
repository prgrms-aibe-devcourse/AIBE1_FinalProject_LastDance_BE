package store.lastdance.service.expense;

import org.springframework.data.domain.Pageable;
import store.lastdance.dto.expense.*;
import store.lastdance.dto.response.PageWithSummaryResponse;

import java.util.List;
import java.util.UUID;

public interface ExpenseV2QueryService {

    ExpenseResponseDTO getExpenseById(UUID userId, Long expenseId);

    List<GroupShareExpenseResponseDTO> getGroupShareExpenses(UUID userId, ExpenseSearchDTO searchDTO);

    String getReceiptImageUrl(Long expenseId, UUID userId);

    MonthlyExpenseTrendResponseDTO getPersonalExpenseTrend(UUID userId, ExpenseSearchDTO searchDTO);

    MonthlyExpenseTrendResponseDTO getGroupExpenseTrend(UUID userId, UUID groupId, ExpenseSearchDTO searchDTO);

    PageWithSummaryResponse<GroupShareExpenseResponseDTO> getGroupShareExpensesWithPaging(
            UUID userId, UUID groupId, ExpenseSearchDTO searchDTO, Pageable pageable
    );

    PageWithSummaryResponse<CombinedExpenseResponseDTO> getCombinedExpenses(
            UUID userId, ExpenseSearchDTO searchDTO, Pageable pageable
    );

    PageWithSummaryResponse<ExpenseResponseDTO> getGroupExpensesWithStats(UUID userId, UUID groupId, ExpenseSearchDTO searchDTO, Pageable pageable);

    PageWithSummaryResponse<ExpenseAnalysisHistoryDTO> getExpenseAnalysisHistory(UUID userId, Pageable pageable);
}
