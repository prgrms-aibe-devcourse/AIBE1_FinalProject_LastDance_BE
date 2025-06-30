package store.lastdance.service.expense;

import store.lastdance.dto.expense.CreateExpenseRequestDTO;
import store.lastdance.dto.expense.ExpenseResponseDTO;
import store.lastdance.dto.expense.GroupShareExpenseResponseDTO;
import store.lastdance.dto.expense.UpdateExpenseRequestDTO;

import java.util.List;
import java.util.UUID;

public interface ExpenseService {

    // 지출 생성
    ExpenseResponseDTO createExpense(UUID userId, CreateExpenseRequestDTO requestDTO);

    // 지출 상세 조회
    ExpenseResponseDTO getExpenseById(UUID userId, Long expenseId);

    // 지출 수정
    ExpenseResponseDTO updateExpense(UUID userId, Long expenseId, UpdateExpenseRequestDTO requestDTO);

    // 지출 삭제
    void deleteExpense(UUID userId, Long expenseId);

    // 그룹 공유 지출 조회 (SHARE)
    List<GroupShareExpenseResponseDTO> getGroupShareExpenses(UUID userId, int year, int month);

    // 개인 지출 조회 (PERSONAL)
    List<ExpenseResponseDTO> getPersonalExpenses(UUID userId, int year, int month, String category, String search);

    // 그룹 지출 조회 (GROUP)
    List<ExpenseResponseDTO> getGroupExpenses(UUID userId, UUID groupId, int year, int month);
}
