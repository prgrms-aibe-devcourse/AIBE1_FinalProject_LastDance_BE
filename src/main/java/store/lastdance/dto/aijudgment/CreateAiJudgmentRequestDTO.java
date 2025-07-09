package store.lastdance.dto.aijudgment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class CreateAiJudgmentRequestDTO {
    @NotEmpty
    private Map<String, String> situations; // Map으로 유지
}