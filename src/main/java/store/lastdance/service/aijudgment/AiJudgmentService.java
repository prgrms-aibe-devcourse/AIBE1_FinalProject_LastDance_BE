package store.lastdance.service.aijudgment;

import store.lastdance.dto.aijudgment.CreateAiJudgmentRequestDTO;
import store.lastdance.dto.aijudgment.AiJudgmentResponseDTO;

import java.util.List;
import java.util.UUID;

public interface AiJudgmentService {
    AiJudgmentResponseDTO judgeConflict(CreateAiJudgmentRequestDTO request, UUID userId);
    String toggleFeedback(UUID judgmentId, UUID userId, String type);
    List<AiJudgmentResponseDTO> getAiJudgmentHistory(UUID userId);
    void deleteAiJudgment(UUID judgmentId, UUID userId);
}