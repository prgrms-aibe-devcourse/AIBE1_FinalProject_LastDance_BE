package store.lastdance.dto.aijudgment;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class AiJudgmentResponseDTO {
    private String judgmentResult;
    private String judgmentId;
    private Map<String, String> situations;
}