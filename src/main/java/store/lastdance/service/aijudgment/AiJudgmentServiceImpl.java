package store.lastdance.service.aijudgment;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import store.lastdance.dto.aijudgment.CreateAiJudgmentRequestDTO;
import store.lastdance.dto.aijudgment.AiJudgmentResponseDTO;
import store.lastdance.domain.aijudgment.AiJudgment;
import store.lastdance.domain.aijudgment.JudgmentType;
import store.lastdance.repository.aijudement.AiJudgmentRepository;
import store.lastdance.external.gemini.GeminiApiClient;

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
                .type(JudgmentType.CONFLICT)
                .situation(request.getSituation())
                .judgmentResult(result)
                .build();

        aiJudgmentRepository.save(judgment);

        return AiJudgmentResponseDTO.builder()
                .judgmentResult(result)
                .build();
    }
}
