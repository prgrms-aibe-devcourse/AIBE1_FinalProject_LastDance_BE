package store.lastdance.service.expense;

import store.lastdance.dto.expense.AnalyzeExpenseResponseDTO;

public interface ExpenseAnalyzer {
    AnalyzeExpenseResponseDTO.Suggestion analyzerExpenseData(String expenseJson);
}