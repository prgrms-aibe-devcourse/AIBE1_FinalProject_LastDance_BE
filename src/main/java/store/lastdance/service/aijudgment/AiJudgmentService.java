// Service Interface
package store.lastdance.service.aijudgment;

import store.lastdance.dto.aijudgment.CreateAiJudgmentRequestDTO;
import store.lastdance.dto.aijudgment.AiJudgmentResponseDTO;

import java.util.UUID;

public interface AiJudgmentService {
    AiJudgmentResponseDTO judgeConflict(CreateAiJudgmentRequestDTO request, UUID userId);
}