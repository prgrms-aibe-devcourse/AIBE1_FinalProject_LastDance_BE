package store.lastdance.service.aijudgment;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import store.lastdance.dto.aijudgment.CreateAiJudgmentRequestDTO;
import store.lastdance.dto.aijudgment.AiJudgmentResponseDTO;
import store.lastdance.domain.aijudgment.AiJudgment;
import store.lastdance.repository.aijudement.AiJudgmentRepository;
import store.lastdance.util.gemini.GeminiApiClient;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class AiJudgmentServiceImpl implements AiJudgmentService {

    private final GeminiApiClient geminiApiClient;
    private final AiJudgmentRepository aiJudgmentRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional
    public AiJudgmentResponseDTO judgeConflict(CreateAiJudgmentRequestDTO request, UUID userId) {
        Map<String, String> situations = request.getSituations();
        String situationA = situations.getOrDefault("A", "");
        String situationB = situations.getOrDefault("B", "");

        if (situationA.isEmpty() || situationB.isEmpty()) {
            return AiJudgmentResponseDTO.builder()
                    .judgmentResult("상황을 조금 더 구체적으로 설명해 주세요.")
                    .judgmentId(null)
                    .situations(situations)
                    .build();
        }

        String combinedSituations = situations.entrySet().stream()
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .collect(Collectors.joining("\n"));

        String moderationResult = geminiApiClient.moderateContent(combinedSituations).block();

        if ("UNSAFE".equals(moderationResult)) {
            return AiJudgmentResponseDTO.builder()
                    .judgmentResult("보안 정책 위반으로 요청을 처리할 수 없습니다.")
                    .judgmentId(null)
                    .situations(situations)
                    .build();
        }

        String judgmentResult = geminiApiClient.getJudgmentResult(situations).block();

        try {
            // 여기에서 situations Map을 JSON 문자열로 변환하여 저장합니다.
            String situationJson = objectMapper.writeValueAsString(request.getSituations());
            AiJudgment judgment = AiJudgment.builder()
                    .judgmentId(UUID.randomUUID())
                    .userId(userId)
                    .situation(situationJson) // JSON 문자열 저장
                    .judgmentResult(judgmentResult)
                    .build();

            aiJudgmentRepository.save(judgment);

            return AiJudgmentResponseDTO.builder()
                    .judgmentResult(judgmentResult)
                    .judgmentId(judgment.getJudgmentId().toString())
                    .situations(request.getSituations()) // 응답 DTO에는 원본 Map을 사용
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("AI 판단 결과를 저장하는 중 오류가 발생했습니다.", e);
        }
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
                judgment.feedback(false, false, null);
                return "좋아요를 취소했습니다.";
            } else {
                judgment.feedback(true, false, null);
                return "좋아요를 반영했습니다.";
            }
        } else if ("down".equalsIgnoreCase(type)) {
            if (isDown) {
                judgment.feedback(false, false, null);
                return "싫어요를 취소했습니다.";
            } else {
                judgment.feedback(false, true, null);
                return "싫어요를 반영했습니다.";
            }
        } else {
            throw new IllegalArgumentException("type은 'up' 또는 'down'이어야 합니다.");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiJudgmentResponseDTO> getAiJudgmentHistory(UUID userId) {
        List<AiJudgment> judgments = aiJudgmentRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return judgments.stream().map(judgment -> {
            Map<String, String> situationsMap;
            try {
                // 저장된 JSON 문자열을 Map으로 역직렬화합니다.
                situationsMap = objectMapper.readValue(judgment.getSituation(), new TypeReference<Map<String, String>>() {});
            } catch (Exception e) {
                // 역직렬화 실패 시 오류 메시지를 반환합니다. (이전 데이터에서 발생)
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