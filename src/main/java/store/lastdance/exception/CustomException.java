package store.lastdance.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class CustomException extends RuntimeException {
    private final ErrorCode errorCode;
    private final HttpStatus httpStatus;

    public CustomException(String message, HttpStatus httpStatus) {
        super(message);
        this.errorCode = null;
        this.httpStatus = httpStatus;
    }

    public CustomException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.httpStatus = errorCode.getHttpStatus();
    }

    public CustomException(String message, Throwable cause, HttpStatus httpStatus) {
        super(message, cause);
        this.errorCode = null;
        this.httpStatus = httpStatus;
    }
}
