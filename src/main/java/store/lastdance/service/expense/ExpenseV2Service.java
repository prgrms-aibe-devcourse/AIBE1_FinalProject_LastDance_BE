package store.lastdance.service.expense;

import org.springframework.web.multipart.MultipartFile;
import store.lastdance.dto.expense.CreateGroupExpenseRequestDTO;
import store.lastdance.dto.expense.CreatePersonalExpenseRequestDTO;
import store.lastdance.dto.expense.ExpenseResponseDTO;
import store.lastdance.dto.expense.UpdateExpenseRequestDTO;

import java.util.UUID;

public interface ExpenseV2Service {

    ExpenseResponseDTO createPersonalExpense(UUID userId, CreatePersonalExpenseRequestDTO requestDTO, MultipartFile receiptFile);

    ExpenseResponseDTO createGroupExpense(UUID userId, CreateGroupExpenseRequestDTO requestDTO, MultipartFile receiptFile);

    ExpenseResponseDTO updateExpense(UUID userId, Long expenseId, UpdateExpenseRequestDTO requestDTO, MultipartFile receiptFile);

    void deleteExpense(UUID userId, Long expenseId);

    void deleteReceiptImage(Long expenseId, UUID userId);

}
