package store.lastdance.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import store.lastdance.dto.common.ErrorResponseDTO;

/**
 * 전역 예외 처리기
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {


    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleUserNotFoundException(UserNotFoundException e) {
        log.warn("User not found: {}", e.getMessage());
        ErrorResponseDTO errorResponse = ErrorResponseDTO.of("USER_NOT_FOUND", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }


    @ExceptionHandler(ExpiredTokenException.class)
    public ResponseEntity<ErrorResponseDTO> handleExpiredTokenException(ExpiredTokenException e) {
        log.warn("Expired token: {}", e.getMessage());
        ErrorResponseDTO errorResponse = ErrorResponseDTO.of("EXPIRED_TOKEN", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ErrorResponseDTO> handleInvalidTokenException(InvalidTokenException e) {
        log.warn("Invalid token: {}", e.getMessage());
        ErrorResponseDTO errorResponse = ErrorResponseDTO.of("INVALID_TOKEN", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    @ExceptionHandler(TokenException.class)
    public ResponseEntity<ErrorResponseDTO> handleTokenException(TokenException e) {
        log.warn("Token error: {}", e.getMessage());
        ErrorResponseDTO errorResponse = ErrorResponseDTO.of("TOKEN_ERROR", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseDTO> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("Illegal argument: {}", e.getMessage());
        ErrorResponseDTO errorResponse = ErrorResponseDTO.of("INVALID_REQUEST", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleGenericException(Exception e) {
        log.error("Unexpected error occurred", e);
        ErrorResponseDTO errorResponse = ErrorResponseDTO.of("INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다.");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

}
