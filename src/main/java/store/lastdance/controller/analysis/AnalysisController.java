package store.lastdance.controller.analysis;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import store.lastdance.aspect.RateLimit;
import store.lastdance.dto.analysis.AnalyzeExpenseRequestDTO;
import store.lastdance.dto.analysis.AnalyzeExpenseResponseDTO;
import store.lastdance.dto.analysis.ExpenseAnalysisHistoryDTO;
import store.lastdance.dto.response.ApiResponse;
import store.lastdance.dto.response.PageWithSummaryResponse;
import store.lastdance.security.oauth.CustomOAuth2User;
import store.lastdance.service.analysis.AnalysisService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/analysis")
@RequiredArgsConstructor
@Tag(name = "Analysis", description = "AI 지출 분석 API")
public class AnalysisController {

    private final AnalysisService analysisService;

    @PostMapping("/expenses")
    @RateLimit // 30초에 1번만 요청 가능
    @Operation(summary = "LLM 지출 분석 요청", description = "지정된 기간의 지출 내역을 LLM을 통해 분석")
    public ResponseEntity<ApiResponse<AnalyzeExpenseResponseDTO>> analyzeExpenses(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomOAuth2User oAuth2User,
            @Valid @RequestBody AnalyzeExpenseRequestDTO requestDTO
    ) {
        UUID userId = oAuth2User.getUserId();
        AnalyzeExpenseResponseDTO response = analysisService.analyzeExpenses(userId, requestDTO);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/expenses/{historyId}/feedback")
    @Operation(summary = "LLM 지출 분석 피드백", description = "LLM 지출분석 결과에 대해 피드백(좋아요/싫어요) 를 남깁니다.")
    public ResponseEntity<ApiResponse<String>> feedbackAnalyzeExpense(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomOAuth2User oAuth2User,
            @PathVariable Long historyId,
            @RequestParam String type
    ) {
        UUID userId = oAuth2User.getUserId();
        String result = analysisService.toggleFeedback(historyId, userId, type);
        return ResponseEntity.ok(ApiResponse.success(result));
    }


    @GetMapping("/expenses/history")
    @Operation(summary = "LLM 지출 분석 내역 조회", description = "사용자의 전체 지출 분석 내역을 최신순으로 조회 (페이징 포함)")
    public ResponseEntity<ApiResponse<PageWithSummaryResponse<ExpenseAnalysisHistoryDTO>>> getAnalysisHistoryList(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomOAuth2User oAuth2User,
            @PageableDefault(
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            ) Pageable pageable
    ) {
        UUID userId = oAuth2User.getUserId();
        PageWithSummaryResponse<ExpenseAnalysisHistoryDTO> response = analysisService.getExpenseAnalysisHistory(userId, pageable);

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
