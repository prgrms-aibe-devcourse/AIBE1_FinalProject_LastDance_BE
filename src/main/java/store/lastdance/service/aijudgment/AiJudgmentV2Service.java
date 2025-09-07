package store.lastdance.service.aijudgment;

import store.lastdance.dto.aijudgment.CreateAiJudgmentRequestDTO;
import store.lastdance.dto.aijudgment.AiJudgmentResponseDTO;

import java.util.UUID;

public interface AiJudgmentV2Service {
    AiJudgmentResponseDTO judgeConflict(CreateAiJudgmentRequestDTO request, UUID userId);
    String toggleFeedback(UUID judgmentId, UUID userId, String type);
    void deleteAiJudgment(UUID judgmentId, UUID userId);
}