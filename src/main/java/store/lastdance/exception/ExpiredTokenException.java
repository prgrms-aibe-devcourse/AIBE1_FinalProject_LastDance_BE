package store.lastdance.exception;

/**
 * 토큰이 만료되었을 때 발생하는 예외
 */
public class ExpiredTokenException extends TokenException {
    public ExpiredTokenException(String message) {
        super(message);
    }
    
    public ExpiredTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
