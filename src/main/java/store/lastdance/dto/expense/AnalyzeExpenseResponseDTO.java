package store.lastdance.dto.expense;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

/**
 8  * LLM 지출 분석 API (POST /api/expenses/analyze)의 응답을 위한 DTO입니다.
 9  * <p>
 10  * 서버는 LLM으로부터 받은 분석 결과를 이 DTO 형식으로 가공하여
 11  * 클라이언트(프론트엔드)에게 반환합니다.
 12  * Java의 record를 사용하여 불변(immutable) 객체로 간결하게 정의합니다.
 13  * </p>
 14  *
 15  * @param summary             지출 내역에 대한 전반적인 텍스트 요약.
 16  *                            <p>예: "지난 한 주간 식비에 가장 많은 돈을 사용하셨으며, 특히 배달 음식 지출이 잦았습니다."</p>
 17  * @param spendingTrends      카테고리별 지출 경향, 금액 등 구조화된 데이터.
 18  *                            <p>클라이언트에서 차트나 그래프를 시각화하는 데 사용할 수 있습니다.
 19  *                            예: {"식비": 50000, "교통비": 30000, "카페": 15000}</p>
 20  * @param savingTips          LLM이 제안하는 구체적인 절약 팁 목록.
 21  *                            <p>예: ["주 1회는 직접 요리해보기", "출퇴근 시 대중교통 이용하기"]</p>
 22  * @param goalSettingAdvice   다음 재정 목표 설정을 위한 조언 텍스트.
 23  *                            <p>예: "다음 달 식비를 10% 줄이는 것을 목표로 설정해보세요."</p>
 24  * @param analysisDate        분석이 수행된 기간을 나타내는 문자열.
 25  *                            <p>클라이언트에서 "분석 기간: 2024-07-01 ~ 2024-07-07" 과 같이 표시하는 데 사용할 수 있습니다.</p>
 26  * @param llmModelUsed        분석에 사용된 LLM 모델의 이름. (디버깅 및 투명성 확보용)
 27  *                            <p>예: "gemini-1.5-flash"</p>
 28  *
 29  * @see store.lastdance.util.gemini.GeminiExpenseAnalyzer#parseExpenseAnalysisResponse(String)
 30  *      GeminiExpenseAnalyzer에서 LLM의 응답을 이 DTO로 파싱하는 로직을 구현하게 됩니다.
 31  */

@Schema(description = "LLM 분석 응답")
public record AnalyzeExpenseResponseDTO(
        String summary,
        Map<String,Object> spendingTrends,
        List<String> savingTips,
        String goalSettingAdvice,
        String analysisDate,
        String llmModelUsed
) {}
