package store.lastdance.dto.admin;

import java.util.List;

public record AiJudgmentResponseDTO(
        List<AiJudgmentDTO> aiJudgments,
        PaginationDTO pagination
) {
}
