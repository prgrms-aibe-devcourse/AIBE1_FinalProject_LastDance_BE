package store.lastdance.dto.aijudgment;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateAiJudgmentRequestDTO {
    @NotBlank
    private String situation;
}