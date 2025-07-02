package store.lastdance.service.aijudgment;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import store.lastdance.dto.aijudgment.CreateAiJudgmentRequestDTO;
import store.lastdance.dto.aijudgment.AiJudgmentResponseDTO;
import store.lastdance.domain.aijudgment.AiJudgment;
import store.lastdance.domain.aijudgment.JudgmentType;
import store.lastdance.repository.aijudement.AiJudgmentRepository;
import store.lastdance.util.gemini.GeminiApiClient;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AiJudgmentServiceImpl implements AiJudgmentService {

    private final GeminiApiClient geminiApiClient;
    private final AiJudgmentRepository aiJudgmentRepository;

    @Override
    public AiJudgmentResponseDTO judgeConflict(CreateAiJudgmentRequestDTO request, UUID userId) {
        String result = geminiApiClient.getJudgmentRatio(request.getSituation());

        AiJudgment judgment = AiJudgment.builder()
                .judgmentId(UUID.randomUUID())
                .userId(userId)
                .situation(request.getSituation())
                .judgmentResult(result)
                .build();

        aiJudgmentRepository.save(judgment);

        return AiJudgmentResponseDTO.builder()
                .judgmentResult(result)
                .judgmentId(judgment.getJudgmentId().toString()) // ✅ 추가
                .build();
    }


    @Override
    @Transactional
    public String toggleFeedback(UUID judgmentId, UUID userId, String type) {
        AiJudgment judgment = aiJudgmentRepository.findById(judgmentId)
                .orElseThrow(() -> new IllegalArgumentException("해당 판단을 찾을 수 없습니다."));

        if (!judgment.getUserId().equals(userId)) {
            throw new IllegalArgumentException("해당 판단에 대한 피드백 권한이 없습니다.");
        }

        boolean isUp = Boolean.TRUE.equals(judgment.getUp());
        boolean isDown = Boolean.TRUE.equals(judgment.getDown());

        if ("up".equalsIgnoreCase(type)) {
            if (isUp) {
                judgment.feedback(false, false, null); // 좋아요 취소
                return "좋아요를 취소했습니다.";
            } else {
                judgment.feedback(true, false, null); // 좋아요 누르고 싫어요 해제
                return "좋아요를 반영했습니다.";
            }
        } else if ("down".equalsIgnoreCase(type)) {
            if (isDown) {
                judgment.feedback(false, false, null); // 싫어요 취소
                return "싫어요를 취소했습니다.";
            } else {
                judgment.feedback(false, true, null); // 싫어요 누르고 좋아요 해제
                return "싫어요를 반영했습니다.";
            }
        } else {
            throw new IllegalArgumentException("type은 'up' 또는 'down'이어야 합니다.");
        }
    }

}
