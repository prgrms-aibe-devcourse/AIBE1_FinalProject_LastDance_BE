package store.lastdance.service.expense;

import org.springframework.lang.Nullable;
import org.springframework.web.multipart.MultipartFile;
import store.lastdance.dto.expense.CreateGroupExpenseRequestDTO;
import store.lastdance.dto.expense.CreatePersonalExpenseRequestDTO;
import store.lastdance.dto.expense.ExpenseResponseDTO;
import store.lastdance.dto.expense.UpdateExpenseRequestDTO;

import java.util.UUID;

public interface ExpenseV2Service {

    ExpenseResponseDTO createPersonalExpense(UUID userId, CreatePersonalExpenseRequestDTO requestDTO, @Nullable MultipartFile receiptFile);

    ExpenseResponseDTO createGroupExpense(UUID userId, CreateGroupExpenseRequestDTO requestDTO, @Nullable MultipartFile receiptFile);

    ExpenseResponseDTO updateExpense(UUID userId, Long expenseId, UpdateExpenseRequestDTO requestDTO, @Nullable MultipartFile receiptFile);

    void deleteExpense(UUID userId, Long expenseId);

    void deleteReceiptImage(UUID userId, Long expenseId);

}
