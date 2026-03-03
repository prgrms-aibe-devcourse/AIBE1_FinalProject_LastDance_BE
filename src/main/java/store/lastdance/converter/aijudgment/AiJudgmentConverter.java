package store.lastdance.converter.aijudgment;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import store.lastdance.domain.aijudgment.AiJudgment;
import store.lastdance.dto.aijudgment.AiJudgmentResponseDTO;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class AiJudgmentConverter {

    private final ObjectMapper objectMapper;

    public AiJudgment toEntity(UUID userId, String situationJson, String judgmentResult) {
        return AiJudgment.builder()
                .judgmentId(UUID.randomUUID())
                .userId(userId)
                .situation(situationJson)
                .judgmentResult(judgmentResult)
                .build();
    }

    public AiJudgmentResponseDTO toResponseDTO(AiJudgment judgment) {
        Map<String, String> situationsMap;
        try {
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
    }

    public AiJudgmentResponseDTO toResponseDTO(AiJudgment judgment, Map<String, String> situations) {
        return AiJudgmentResponseDTO.builder()
                .judgmentResult(judgment.getJudgmentResult())
                .judgmentId(judgment.getJudgmentId().toString())
                .situations(situations)
                .build();
    }
}
