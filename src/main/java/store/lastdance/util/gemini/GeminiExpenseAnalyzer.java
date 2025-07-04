package store.lastdance.util.gemini;

import org.springframework.stereotype.Component;

/**
 * Gemini API를 사용하여 사용자의 지출 내역을 분석하는 클라이언트 클래스입니다.
 *
 * <p>이 클래스는 Spring WebClient를 사용하여 Google Gemini API의 REST 엔드포인트를 직접 호출합니다.
 * 지출 내역 데이터를 받아 분석을 위한 프롬프트를 생성하고, API에 전송한 뒤,
 * 반환된 JSON 응답을 파싱하여 구조화된 분석 결과(DTO)를 제공하는 역할을 합니다.
 * </p>
 *
 * @see store.lastdance.service.expense.ExpenseService ExpenseService에서 이 클래스를 주입받아 사용하게 됩니다.
 * @see store.lastdance.util.gemini.GeminiApiClient GeminiApiClient와 유사한 구조를 가집니다.
 */
@Component
public class GeminiExpenseAnalyzer {}