package store.lastdance.exception;

/**
 * JWT 토큰 관련 예외들의 부모 클래스
 */
public class TokenException extends RuntimeException {
    public TokenException(String message) {
        super(message);
    }
    
    public TokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
