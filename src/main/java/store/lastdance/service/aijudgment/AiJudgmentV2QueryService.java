package store.lastdance.service.aijudgment;

import store.lastdance.dto.aijudgment.AiJudgmentResponseDTO;

import java.util.List;
import java.util.UUID;

public interface AiJudgmentV2QueryService {
    List<AiJudgmentResponseDTO> getAiJudgmentHistory(UUID userId);
}
