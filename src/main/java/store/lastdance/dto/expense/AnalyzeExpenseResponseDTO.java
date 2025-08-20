package store.lastdance.dto.expense;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "LLM 지출 분석 결과 응답")
public record AnalyzeExpenseResponseDTO(
    @Schema(description = "자동 저장된 분석 내역 ID")
    Long historyId,

    @Schema(description = "예산 사용률 정보")
    BudgetUsage budgetUsage,

    @Schema(description = "일평균 지출 정보")
    DailySpending dailySpending,

    @Schema(description = "분석 결과 요약")
    AnalysisResult analysisResult,

    @Schema(description = "카테고리별 상세 분석 목록")
    List<CategoryDetail> categoryDetails
) {

    @Schema(description = "예산 사용률")
    public record BudgetUsage(
        @Schema(description = "사용률 (백분율)", example = "18.1")
        double percentage,

        @Schema(description = "현재 사용액", example = "361445")
        BigDecimal currentSpending,

        @Schema(description = "총 예산", example = "2000000")
        BigDecimal totalBudget
    ) {}

    @Schema(description = "일평균 지출")
    public record DailySpending(
        @Schema(description = "현재까지의 일평균 지출액", example = "51635")
        BigDecimal averageSoFar,

        @Schema(description = "월말 예상 지출액 (현재 패턴 기준)", example = "1600685")
        BigDecimal estimatedEom
    ) {}

    @Schema(description = "분석 결과 및 개선 제안")
    public record AnalysisResult(
        @Schema(description = "핵심 소비 패턴 요약", example = "쇼핑 지출 집중")
        String mainFinding,

        @Schema(description = "개선 제안")
        Suggestion suggestion
    ) {}

    @Schema(description = "개선 제안 상세")
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Suggestion(
        @Schema(description = "제목", example = "자동 저축 설정")
        String title,

        @Schema(description = "예상 효과", example = "연간 목표 달성률 40% 향상")
        String effect,

        @Schema(description = "난이도", example = "쉬움")
        String difficulty,
        @Schema(description = "구체적인 행동 제안", example = "매월 고정 금액을 자동으로 저축하는 습관을 만들어보세요.")
        String description
    ) {}

    @Schema(description = "카테고리별 상세 분석")
    public record CategoryDetail(
        @Schema(description = "카테고리명", example = "쇼핑")
        String category,

        @Schema(description = "지출 비중 (백분율)", example = "55.4")
        double percentage,

        @Schema(description = "해당 카테고리 총 지출액", example = "200220")
        BigDecimal totalAmount,

        @Schema(description = "해당 카테고리 지출 건수", example = "3")
        int transactionCount
    ) {}
}

