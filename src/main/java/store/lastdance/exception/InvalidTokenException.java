package store.lastdance.exception;

/**
 * 토큰이 유효하지 않을 때 발생하는 예외 (변조, 잘못된 형식 등)
 */
public class InvalidTokenException extends TokenException {
    public InvalidTokenException(String message) {
        super(message);
    }
    
    public InvalidTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
