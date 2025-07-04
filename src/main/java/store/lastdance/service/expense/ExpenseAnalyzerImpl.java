package store.lastdance.service.expense;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import store.lastdance.dto.expense.AnalyzeExpenseResponseDTO;

/**
 * LLM을 사용하여 사용자의 지출 내역을 분석하는 클라이언트 클래스입니다.
 *
 * <p>이 클래스는 LLM 서비스의 API를 호출하여 지출 내역 데이터를 분석하고,
 * 반환된 응답을 파싱하여 구조화된 분석 결과(DTO)를 제공하는 역할을 합니다.
 * </p>
 *
 * @see ExpenseService ExpenseService에서 이 클래스를 주입받아 사용하게 됩니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExpenseAnalyzerImpl implements ExpenseAnalyzer {
    @Override
    public AnalyzeExpenseResponseDTO analyzerExpenseData(String expenseJson) {
        return null;
    }
}
