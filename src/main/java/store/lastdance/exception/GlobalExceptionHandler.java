package store.lastdance.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import store.lastdance.dto.common.ErrorResponseDTO;

/**
 * 전역 예외 처리기
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponseDTO> handleCustomException(CustomException e, WebRequest request) {

        ErrorResponseDTO errorResponseDTO = ErrorResponseDTO.builder()
                .status(e.getHttpStatus().value())
                .error(e.getHttpStatus().getReasonPhrase())
                .message(e.getMessage())
                .path(request.getDescription(false).substring(4))
                .build();

        return ResponseEntity.status(errorResponseDTO.status()).body(errorResponseDTO);
    }

}
