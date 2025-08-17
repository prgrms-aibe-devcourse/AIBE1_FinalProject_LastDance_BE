package store.lastdance.service.expense;

import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;
import store.lastdance.dto.expense.*;
import store.lastdance.dto.response.PageWithSummaryResponse;

import java.util.List;
import java.util.UUID;

public interface ExpenseV2Service {

    // 개인 지출 생성
    ExpenseResponseDTO createPersonalExpense(UUID userId, CreatePersonalExpenseRequestDTO requestDTO, MultipartFile receiptFile);

    // 그룹 지출 생성
    ExpenseResponseDTO createGroupExpense(UUID userId, CreateGroupExpenseRequestDTO requestDTO, MultipartFile receiptFile);

    // 지출 상세 조회
    ExpenseResponseDTO getExpenseById(UUID userId, Long expenseId);

    // 지출 수정
    ExpenseResponseDTO updateExpense(UUID userId, Long expenseId, UpdateExpenseRequestDTO requestDTO, MultipartFile receiptFile);

    // 지출 삭제
    void deleteExpense(UUID userId, Long expenseId);

    // 그룹 공유 지출 조회 (SHARE)
    List<GroupShareExpenseResponseDTO> getGroupShareExpenses(UUID userId, ExpenseSearchDTO searchDTO);

    // 영수증 이미지 Pre-signed URL 조회
    String getReceiptImageUrl(Long expenseId, UUID userId);

    // 영수증만 삭제
    void deleteReceiptImage(Long expenseId, UUID userId);

    // 개인 지출 월별 추이 조회
    MonthlyExpenseTrendResponseDTO getPersonalExpenseTrend(UUID userId, ExpenseSearchDTO searchDTO);

    // 그룹 지출 월별 추이 조회
    MonthlyExpenseTrendResponseDTO getGroupExpenseTrend(UUID userId, UUID groupId, ExpenseSearchDTO searchDTO);

    // 특정 그룹 분담금 페이징 조회
    PageWithSummaryResponse<GroupShareExpenseResponseDTO> getGroupShareExpensesWithPaging(
            UUID userId, UUID groupId, ExpenseSearchDTO searchDTO, Pageable pageable
    );

    // 개인 지출 + 분담금 내역 조회
    PageWithSummaryResponse<CombinedExpenseResponseDTO> getCombinedExpenses(
            UUID userId, ExpenseSearchDTO searchDTO, Pageable pageable
    );

    // 그룹 지출 + 통계 조회
    PageWithSummaryResponse<ExpenseResponseDTO> getGroupExpensesWithStats(UUID userId, UUID groupId, ExpenseSearchDTO searchDTO, Pageable pageable);

    // LLM 지출 분석 응답
    AnalyzeExpenseResponseDTO analyzeExpenses(UUID userId, AnalyzeExpenseRequestDTO requestDTO);
    // LLM 지출 분석 내역 조회
    PageWithSummaryResponse<ExpenseAnalysisHistoryDTO> getExpenseAnalysisHistory(UUID userId, Pageable pageable);
    // LLM 지출 분석 내역 저장
//    void saveExpenseAnalysisHistory(UUID userId, AnalyzeExpenseRequestDTO requestDTO,AnalyzeExpenseResponseDTO analysisResponseDTO);
    // LLM 유저 평가
    String toggleFeedback(Long historyId, UUID userId, String type);
}
