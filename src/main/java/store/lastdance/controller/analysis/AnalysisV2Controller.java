package store.lastdance.controller.analysis;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import store.lastdance.aspect.RateLimit;
import store.lastdance.domain.analysis.FeedbackType;
import store.lastdance.dto.analysis.AnalyzeExpenseRequestDTO;
import store.lastdance.dto.analysis.AnalyzeExpenseResponseDTO;
import store.lastdance.dto.analysis.ExpenseAnalysisHistoryDTO;
import store.lastdance.dto.response.ApiResponse;
import store.lastdance.dto.response.PageWithSummaryResponse;
import store.lastdance.security.oauth.CustomOAuth2User;
import store.lastdance.service.analysis.AnalysisV2CommandService;
import store.lastdance.service.analysis.AnalysisV2QueryService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v2/analysis")
@RequiredArgsConstructor
@Tag(name = "Analysis V2", description = "AI 지출 분석 V2 API")
@Validated
public class AnalysisV2Controller {

    private final AnalysisV2CommandService analysisV2CommandService;
    private final AnalysisV2QueryService analysisV2QueryService;

    @PostMapping("/expenses")
    @RateLimit // 30초에 1번만 요청 가능
    @Operation(summary = "LLM 지출 분석 요청", description = "지정된 기간의 지출 내역을 LLM을 통해 분석")
    public ResponseEntity<ApiResponse<AnalyzeExpenseResponseDTO>> analyzeExpenses(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomOAuth2User oAuth2User,
            @Valid @RequestBody AnalyzeExpenseRequestDTO requestDTO
    ) {
        UUID userId = oAuth2User.getUserId();
        AnalyzeExpenseResponseDTO response = analysisV2QueryService.analyzeExpenses(userId, requestDTO);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/expenses/{historyId}/feedback")
    @RateLimit // 30초에 1번만 요청 가능
    @Operation(summary = "LLM 지출 분석 피드백 토글", description = "LLM 지출분석 결과에 대해 피드백(좋아요/싫어요)을 남기거나 취소합니다.")
    public ResponseEntity<?> feedbackAnalyzeExpense(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomOAuth2User oAuth2User,
            @PathVariable @Min(1) Long historyId,
            @RequestParam FeedbackType type
    ) {
        UUID userId = oAuth2User.getUserId();
        FeedbackType result = analysisV2CommandService.toggleFeedback(historyId, userId, type);

        if (result == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(ApiResponse.success(result));
    }


    @GetMapping("/expenses")
    @Operation(summary = "LLM 지출 분석 내역 조회", description = "사용자의 전체 지출 분석 내역을 최신순으로 조회 (페이징 포함)")
    public ResponseEntity<ApiResponse<PageWithSummaryResponse<ExpenseAnalysisHistoryDTO>>> getAnalysisHistoryList(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomOAuth2User oAuth2User,
            @PageableDefault(
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            ) Pageable pageable
    ) {
        UUID userId = oAuth2User.getUserId();
        PageWithSummaryResponse<ExpenseAnalysisHistoryDTO> response = analysisV2QueryService.getExpenseAnalysisHistory(userId, pageable);

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
