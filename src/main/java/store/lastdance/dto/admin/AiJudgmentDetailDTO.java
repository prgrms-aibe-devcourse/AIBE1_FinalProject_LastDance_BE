package store.lastdance.dto.admin;

import java.time.LocalDateTime;
import java.util.UUID;

public record AiJudgmentDetailDTO(
        UUID judgmentId,
        AdminPageUserDTO user,
        AdminPageGroupDTO group,
        String originalRequest,
        String aiResponse,
        String userRating,
        String judgmentComment,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
