package store.lastdance.service.aijudgment;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import store.lastdance.converter.aijudgment.AiJudgmentConverter;
import store.lastdance.domain.aijudgment.AiJudgment;
import store.lastdance.dto.aijudgment.AiJudgmentResponseDTO;
import store.lastdance.repository.aijudgment.AiJudgmentRepository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiJudgmentV2QueryServiceImpl implements AiJudgmentV2QueryService {

    private final AiJudgmentRepository aiJudgmentRepository;
    private final AiJudgmentConverter aiJudgmentConverter;

    @Override
    @Transactional(readOnly = true)
    public List<AiJudgmentResponseDTO> getAiJudgmentHistory(UUID userId) {
        List<AiJudgment> judgments = aiJudgmentRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return judgments.stream()
                .map(aiJudgmentConverter::toResponseDTO)
                .collect(Collectors.toList());
    }
}