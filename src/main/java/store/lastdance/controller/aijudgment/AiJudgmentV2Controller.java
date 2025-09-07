package store.lastdance.controller.aijudgment;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import store.lastdance.dto.aijudgment.AiJudgmentResponseDTO;
import store.lastdance.dto.aijudgment.CreateAiJudgmentRequestDTO;
import store.lastdance.dto.response.ApiResponse;
import store.lastdance.security.oauth.CustomOAuth2User;
import store.lastdance.service.aijudgment.AiJudgmentV2QueryService;
import store.lastdance.service.aijudgment.AiJudgmentV2Service;

import java.util.List;
import java.util.UUID;

@Tag(name = "AI Judgments V2", description = "AI 판단 API V2")
@RestController
@RequestMapping("/api/v2/ai/judgments")
@RequiredArgsConstructor
@Slf4j
public class AiJudgmentV2Controller {

    private final AiJudgmentV2Service aiJudgmentService;
    private final AiJudgmentV2QueryService aiJudgmentV2QueryService;

    @Operation(
            summary = "갈등 상황 판단 요청",
            description = "사용자가 입력한 갈등 상황을 기반으로 AI가 잘잘못 비율을 판단합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping
    public ResponseEntity<ApiResponse<AiJudgmentResponseDTO>> judgeConflict(
            @Valid @RequestBody CreateAiJudgmentRequestDTO request,
            @AuthenticationPrincipal CustomOAuth2User user) {

        log.info("갈등 판단 요청 - 사용자 ID: {}, 상황: {}", user.getUserId(), request.getSituations());

        try {
            AiJudgmentResponseDTO response = aiJudgmentService.judgeConflict(request, user.getUserId());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(response, "판단이 성공적으로 완료되었습니다."));

        } catch (Exception e) {
            log.error("갈등 판단 실패 - 사용자 ID: {}, 에러: {}", user.getUserId(), e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("판단 요청에 실패했습니다: " + e.getMessage()));
        }
    }

    @Operation(
            summary = "AI 판단 결과 피드백 (좋아요/싫어요)",
            description = "사용자가 AI 판단 결과에 대해 좋아요 또는 싫어요를 누를 수 있습니다. 둘 중 하나만 활성화됩니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping("/{judgmentId}/feedback")
    public ResponseEntity<ApiResponse<String>> toggleFeedback(
            @PathVariable UUID judgmentId,
            @RequestParam("type") String type,
            @AuthenticationPrincipal CustomOAuth2User user
    ) {
        try {
            String message = aiJudgmentService.toggleFeedback(judgmentId, user.getUserId(), type);
            return ResponseEntity.ok(ApiResponse.success(null, message));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("요청이 잘못되었습니다: " + e.getMessage()));
        }
    }

    @Operation(
            summary = "사용자 AI 판단 내역 조회",
            description = "현재 로그인한 사용자가 요청했던 AI 판단 내역을 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<AiJudgmentResponseDTO>>> getAiJudgmentHistory(
            @AuthenticationPrincipal CustomOAuth2User user) {
        try {
            List<AiJudgmentResponseDTO> history = aiJudgmentV2QueryService.getAiJudgmentHistory(user.getUserId());
            return ResponseEntity.ok(ApiResponse.success(history, "AI 판단 내역을 성공적으로 조회했습니다."));
        } catch (Exception e) {
            log.error("AI 판단 내역 조회 실패 - 사용자 ID: {}, 에러: {}", user.getUserId(), e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("AI 판단 내역 조회에 실패했습니다: " + e.getMessage()));
        }
    }

    @Operation(
            summary = "AI 판단 내역 삭제",
            description = "특정 AI 판단 내역을 삭제합니다. 해당 내역의 소유자만 삭제할 수 있습니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @DeleteMapping("/history/{judgmentId}/delete")
    public ResponseEntity<ApiResponse<String>> deleteAiJudgment(
            @PathVariable UUID judgmentId,
            @AuthenticationPrincipal CustomOAuth2User user) {
        try {
            aiJudgmentService.deleteAiJudgment(judgmentId, user.getUserId());
            return ResponseEntity.ok(ApiResponse.success(null, "AI 판단 내역이 성공적으로 삭제되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("삭제 요청이 잘못되었습니다: " + e.getMessage()));
        } catch (Exception e) {
            log.error("AI 판단 내역 삭제 실패 - 사용자 ID: {}, 판단 ID: {}, 에러: {}", user.getUserId(), judgmentId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("AI 판단 내역 삭제에 실패했습니다: " + e.getMessage()));
        }
    }
}