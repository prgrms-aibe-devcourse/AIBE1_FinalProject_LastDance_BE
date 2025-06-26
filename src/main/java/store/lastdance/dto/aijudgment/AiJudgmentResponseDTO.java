package store.lastdance.dto.aijudgment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class AiJudgmentResponseDTO {
    private String judgmentResult;
}