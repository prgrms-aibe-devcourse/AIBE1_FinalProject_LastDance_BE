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
import store.lastdance.dto.aijudgment.CreateAiJudgmentRequestDTO;
import store.lastdance.dto.aijudgment.AiJudgmentResponseDTO;
import store.lastdance.dto.response.ApiResponse;
import store.lastdance.security.oauth.CustomOAuth2User;
import store.lastdance.service.aijudgment.AiJudgmentService;

import java.util.UUID;

@Tag(name = "AI Judgments", description = "AI 판단 API")
@RestController
@RequestMapping("/api/v1/ai/judgments")
@RequiredArgsConstructor
@Slf4j
public class AiJudgmentController {

    private final AiJudgmentService aiJudgmentService;

    @Operation(
            summary = "갈등 상황 판단 요청",
            description = "사용자가 입력한 갈등 상황을 기반으로 AI가 잘잘못 비율을 판단합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping
    public ResponseEntity<ApiResponse<AiJudgmentResponseDTO>> judgeConflict(
            @Valid @RequestBody CreateAiJudgmentRequestDTO request,
            @AuthenticationPrincipal CustomOAuth2User user) {

        log.info("갈등 판단 요청 - 사용자 ID: {}, 상황: {}", user.getUserId(), request.getSituation());

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
}