package store.lastdance.service.aijudgment;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import store.lastdance.converter.aijudgment.AiJudgmentConverter;
import store.lastdance.domain.aijudgment.AiJudgment;
import store.lastdance.dto.aijudgment.AiJudgmentResponseDTO;
import store.lastdance.dto.aijudgment.CreateAiJudgmentRequestDTO;
import store.lastdance.repository.aijudgment.AiJudgmentRepository;
import store.lastdance.util.gemini.GeminiApiClient;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiJudgmentV2ServiceImpl implements AiJudgmentV2Service {

    private final GeminiApiClient geminiApiClient;
    private final AiJudgmentRepository aiJudgmentRepository;
    private final AiJudgmentConverter aiJudgmentConverter;
    private final ObjectMapper objectMapper;

    @Override
    public AiJudgmentResponseDTO judgeConflict(CreateAiJudgmentRequestDTO request, UUID userId) {
        Map<String, String> situations = request.getSituations();

        long validParticipantCount = situations.entrySet().stream()
                .filter(entry -> entry.getKey() != null && !entry.getKey().trim().isEmpty())
                .filter(entry -> entry.getValue() != null && !entry.getValue().trim().isEmpty())
                .count();

        if (validParticipantCount < 2) {
            log.warn("갈등 판단 요청 실패: 유효한 참가자 상황이 2명 미만입니다. 현재 {}명. 사용자 ID: {}", validParticipantCount, userId);
            return AiJudgmentResponseDTO.builder()
                    .judgmentResult("정확한 판단을 위해 최소 2명 이상의 참가자와 그들의 입장을 구체적으로 입력해 주세요.")
                    .judgmentId(null)
                    .situations(situations)
                    .build();
        }

        String combinedSituationsForModeration = situations.entrySet().stream()
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .collect(Collectors.joining("\n"));

        String moderationResult = geminiApiClient.moderateContent(combinedSituationsForModeration).block();

        if ("UNSAFE".equals(moderationResult)) {
            log.warn("갈등 판단 요청 보안 정책 위반 감지 - 사용자 ID: {}", userId);
            return AiJudgmentResponseDTO.builder()
                    .judgmentResult("보안 정책 위반으로 요청을 처리할 수 없습니다.")
                    .judgmentId(null)
                    .situations(situations)
                    .build();
        }

        String judgmentResult = geminiApiClient.getJudgmentResult(situations).block();

        try {
            String situationJson = objectMapper.writeValueAsString(request.getSituations());
            return saveAiJudgmentAndConvertToDto(userId, situationJson, judgmentResult, request.getSituations());
        } catch (JsonProcessingException e) {
            log.error("AI 판단 결과를 데이터베이스에 저장하기 전 JSON 직렬화 중 오류가 발생했습니다. 사용자 ID: {}", userId, e);
            throw new RuntimeException("AI 판단 결과를 저장하는 중 오류가 발생했습니다.", e);
        }
    }

    @Transactional
    protected AiJudgmentResponseDTO saveAiJudgmentAndConvertToDto(UUID userId, String situationJson, String judgmentResult, Map<String, String> originalSituations) {
        AiJudgment judgment = aiJudgmentConverter.toEntity(userId, situationJson, judgmentResult);
        aiJudgmentRepository.save(judgment);
        log.info("AI 판단 결과 DB 저장 완료 - judgmentId: {}", judgment.getJudgmentId());
        return aiJudgmentConverter.toResponseDTO(judgment, originalSituations);
    }

    @Override
    @Transactional
    public String toggleFeedback(UUID judgmentId, UUID userId, String type) {
        AiJudgment judgment = aiJudgmentRepository.findById(judgmentId)
                .orElseThrow(() -> new IllegalArgumentException("해당 판단을 찾을 수 없습니다."));

        if (!judgment.getUserId().equals(userId)) {
            throw new IllegalArgumentException("해당 판단에 대한 피드백 권한이 없습니다.");
        }
        if (type == null || type.trim().isEmpty()) {
            throw new IllegalArgumentException("type은 필수입니다.");
        }
        final String normalized = type.trim().toLowerCase();

        boolean isUp = Boolean.TRUE.equals(judgment.getUp());
        boolean isDown = Boolean.TRUE.equals(judgment.getDown());

        if ("up".equals(normalized)) {
            if (isUp) {
                judgment.feedback(false, false, null);
                return "좋아요를 취소했습니다.";
            }
            else {
                judgment.feedback(true, false, null);
                return "좋아요를 반영했습니다.";
            }
        } else if ("down".equals(normalized)) {
            if (isDown) {
                judgment.feedback(false, false, null);
                return "싫어요를 취소했습니다.";
            }
            else {
                judgment.feedback(false, true, null);
                return "싫어요를 반영했습니다.";
            }
        } else {
            throw new IllegalArgumentException("type은 'up' 또는 'down'이어야 합니다.");
        }
    }

    @Override
    @Transactional
    public void deleteAiJudgment(UUID judgmentId, UUID userId) {
        AiJudgment judgment = aiJudgmentRepository.findById(judgmentId)
                .orElseThrow(() -> new IllegalArgumentException("삭제할 AI 판단 내역을 찾을 수 없습니다."));

        if (!judgment.getUserId().equals(userId)) {
            throw new IllegalArgumentException("해당 AI 판단 내역을 삭제할 권한이 없습니다.");
        }

        aiJudgmentRepository.delete(judgment);
        log.info("AI 판단 내역 삭제 완료 - judgmentId: {}", judgment.getJudgmentId());
    }
}