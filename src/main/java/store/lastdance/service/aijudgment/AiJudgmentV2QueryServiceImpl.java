package store.lastdance.service.aijudgment;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import store.lastdance.domain.aijudgment.AiJudgment;
import store.lastdance.dto.aijudgment.AiJudgmentResponseDTO;
import store.lastdance.repository.aijudgment.AiJudgmentRepository;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiJudgmentV2QueryServiceImpl implements AiJudgmentV2QueryService {

    private final AiJudgmentRepository aiJudgmentRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional(readOnly = true)
    public List<AiJudgmentResponseDTO> getAiJudgmentHistory(UUID userId) {
        List<AiJudgment> judgments = aiJudgmentRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return judgments.stream().map(judgment -> {
            Map<String, String> situationsMap;
            try {
                // DB에 저장된 JSON 문자열을 다시 Map으로 변환
                situationsMap = objectMapper.readValue(judgment.getSituation(), new TypeReference<Map<String, String>>() {});
            } catch (Exception e) {
                log.error("AI 판단 내역의 상황 JSON 파싱 실패 - judgmentId: {}, error: {}", judgment.getJudgmentId(), e.getMessage());
                situationsMap = Map.of("error", "상황 정보를 불러올 수 없습니다.");
            }
            return AiJudgmentResponseDTO.builder()
                    .judgmentId(judgment.getJudgmentId().toString())
                    .judgmentResult(judgment.getJudgmentResult())
                    .situations(situationsMap)
                    .build();
        }).collect(Collectors.toList());
    }
}
