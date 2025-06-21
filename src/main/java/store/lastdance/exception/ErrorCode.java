package store.lastdance.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 사용자 관련
    USER_NOT_FOUND("사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    USER_INACTIVE("비활성화된 사용자입니다.", HttpStatus.FORBIDDEN),

    // 토큰 관련
    INVALID_TOKEN("유효하지 않은 토큰입니다.", HttpStatus.UNAUTHORIZED)



    ;
    private final String message;
    private final HttpStatus httpStatus;
}
