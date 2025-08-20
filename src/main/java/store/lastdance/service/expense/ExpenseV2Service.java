package store.lastdance.service.expense;

import org.springframework.web.multipart.MultipartFile;
import store.lastdance.dto.expense.*;

import java.util.UUID;

public interface ExpenseV2Service {

    ExpenseResponseDTO createPersonalExpense(UUID userId, CreatePersonalExpenseRequestDTO requestDTO, MultipartFile receiptFile);

    ExpenseResponseDTO createGroupExpense(UUID userId, CreateGroupExpenseRequestDTO requestDTO, MultipartFile receiptFile);

    ExpenseResponseDTO updateExpense(UUID userId, Long expenseId, UpdateExpenseRequestDTO requestDTO, MultipartFile receiptFile);

    void deleteExpense(UUID userId, Long expenseId);

    void deleteReceiptImage(Long expenseId, UUID userId);

    AnalyzeExpenseResponseDTO analyzeExpenses(UUID userId, AnalyzeExpenseRequestDTO requestDTO);

    String toggleFeedback(Long historyId, UUID userId, String type);
}
