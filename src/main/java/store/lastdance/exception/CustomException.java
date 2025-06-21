package store.lastdance.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class CustomException extends RuntimeException {
    private final HttpStatus httpStatus;

    public CustomException(String message, HttpStatus httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public CustomException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.httpStatus = errorCode.getHttpStatus();
    }

    public CustomException(String message, Throwable cause, HttpStatus httpStatus) {
        super(message, cause);
        this.httpStatus = httpStatus;
    }
}