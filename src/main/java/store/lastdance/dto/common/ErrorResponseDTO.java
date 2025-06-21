package store.lastdance.dto.common;

import java.time.LocalDateTime;

/**
 * 에러 응답 DTO
 */
public record ErrorResponseDTO(
        String code,
        String message,
        LocalDateTime timestamp
) {
    public static ErrorResponseDTO of(String code, String message) {
        return new ErrorResponseDTO(code, message, LocalDateTime.now());
    }
}
