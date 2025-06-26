package store.lastdance.dto.admin;

import java.util.UUID;

public record AiJudgmentDTO(
        UUID judgmentId,
        AdminPageUserDTO user,
        AdminPageGroupDTO group,
        String requestSummary,
        String aiResponse,
        String userRating,
        String judgmentComment,
        String createdAt
) {
}
