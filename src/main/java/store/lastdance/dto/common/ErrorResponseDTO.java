package store.lastdance.dto.common;

import lombok.Builder;

/**
 * 에러 응답 DTO
 */
@Builder
public record ErrorResponseDTO (
        int status,
        String error,
        String message,
        String path
){}