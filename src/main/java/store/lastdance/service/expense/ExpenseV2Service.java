package store.lastdance.service.expense;

import org.springframework.web.multipart.MultipartFile;
import store.lastdance.dto.expense.*;

import java.util.UUID;

public interface ExpenseV2Service {

    // 개인 지출 생성
    ExpenseResponseDTO createPersonalExpense(UUID userId, CreatePersonalExpenseRequestDTO requestDTO, MultipartFile receiptFile);

    // 그룹 지출 생성
    ExpenseResponseDTO createGroupExpense(UUID userId, CreateGroupExpenseRequestDTO requestDTO, MultipartFile receiptFile);

    // 지출 수정
    ExpenseResponseDTO updateExpense(UUID userId, Long expenseId, UpdateExpenseRequestDTO requestDTO, MultipartFile receiptFile);

    // 지출 삭제
    void deleteExpense(UUID userId, Long expenseId);

    // 영수증만 삭제
    void deleteReceiptImage(Long expenseId, UUID userId);

    // LLM 지출 분석 응답
    AnalyzeExpenseResponseDTO analyzeExpenses(UUID userId, AnalyzeExpenseRequestDTO requestDTO);

    // LLM 지출 분석 내역 저장
//    void saveExpenseAnalysisHistory(UUID userId, AnalyzeExpenseRequestDTO requestDTO,AnalyzeExpenseResponseDTO analysisResponseDTO);
    // LLM 유저 평가
    String toggleFeedback(Long historyId, UUID userId, String type);
}
