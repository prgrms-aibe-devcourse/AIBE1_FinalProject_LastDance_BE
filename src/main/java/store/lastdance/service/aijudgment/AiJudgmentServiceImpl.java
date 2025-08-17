package store.lastdance.service.aijudgment;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import store.lastdance.dto.aijudgment.CreateAiJudgmentRequestDTO;
import store.lastdance.dto.aijudgment.AiJudgmentResponseDTO;
import store.lastdance.domain.aijudgment.AiJudgment;
import store.lastdance.repository.aijudgment.AiJudgmentRepository;
import store.lastdance.util.gemini.GeminiApiClient;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiJudgmentServiceImpl implements AiJudgmentService {

    private final GeminiApiClient geminiApiClient;
    private final AiJudgmentRepository aiJudgmentRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional
    public AiJudgmentResponseDTO judgeConflict(CreateAiJudgmentRequestDTO request, UUID userId) {
        Map<String, String> situations = request.getSituations();

        // 유효한 (이름과 상황이 모두 채워진) 참가자의 수를 확인
        long validParticipantCount = situations.entrySet().stream()
                .filter(entry -> entry.getKey() != null && !entry.getKey().trim().isEmpty())
                .filter(entry -> entry.getValue() != null && !entry.getValue().trim().isEmpty())
                .count();

        // 최소 2명 이상의 유효한 참가자 상황이 없으면 에러 반환
        if (validParticipantCount < 2) {
            log.warn("갈등 판단 요청 실패: 유효한 참가자 상황이 2명 미만입니다. 현재 {}명. 사용자 ID: {}", validParticipantCount, userId);
            return AiJudgmentResponseDTO.builder()
                    .judgmentResult("정확한 판단을 위해 최소 2명 이상의 참가자와 그들의 입장을 구체적으로 입력해 주세요.")
                    .judgmentId(null)
                    .situations(situations)
                    .build();
        }

        // Gemini Moderation API 호출을 위한 전체 상황 문자열
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

        // Gemini Judgment API 호출
        String judgmentResult = geminiApiClient.getJudgmentResult(situations).block();

        try {
            // Map<String, String>을 JSON 문자열로 변환하여 DB에 저장
            String situationJson = objectMapper.writeValueAsString(request.getSituations());
            AiJudgment judgment = AiJudgment.builder()
                    .judgmentId(UUID.randomUUID())
                    .userId(userId)
                    .situation(situationJson)
                    .judgmentResult(judgmentResult)
                    .build();

            aiJudgmentRepository.save(judgment);
            log.info("AI 판단 결과 DB 저장 완료 - judgmentId: {}", judgment.getJudgmentId());

            return AiJudgmentResponseDTO.builder()
                    .judgmentResult(judgmentResult)
                    .judgmentId(judgment.getJudgmentId().toString())
                    .situations(request.getSituations())
                    .build();
        } catch (Exception e) {
            log.error("AI 판단 결과를 데이터베이스에 저장하는 중 오류가 발생했습니다. 사용자 ID: {}", userId, e);
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

    @Override
    @Transactional
    public void deleteAiJudgment(UUID judgmentId, UUID userId) {
        AiJudgment judgment = aiJudgmentRepository.findById(judgmentId)
                .orElseThrow(() -> new IllegalArgumentException("삭제할 AI 판단 내역을 찾을 수 없습니다."));

        if (!judgment.getUserId().equals(userId)) {
            throw new IllegalArgumentException("해당 AI 판단 내역을 삭제할 권한이 없습니다.");
        }

        aiJudgmentRepository.delete(judgment);
        log.info("AI 판단 내역 삭제 완료 - judgmentId: {}", judgmentId);
    }
}