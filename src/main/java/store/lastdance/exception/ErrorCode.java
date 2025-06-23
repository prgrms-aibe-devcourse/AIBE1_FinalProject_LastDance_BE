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
    USER_CREATE_FAILED("사용자 생성에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    UNSUPPORTED_OAUTH_PROVIDER("지원하지 않는 소셜 로그인 제공자입니다", HttpStatus.BAD_REQUEST),

    // 토큰 관련
    TOKEN_ERROR("토큰 에러", HttpStatus.UNAUTHORIZED),
    INVALID_TOKEN("유효하지 않은 토큰입니다.", HttpStatus.UNAUTHORIZED),
    INVALID_REFRESH_TOKEN("유효하지 않은 리프레시 토큰입니다.", HttpStatus.UNAUTHORIZED),
    EXPIRED_TOKEN("만료된 토큰입니다.", HttpStatus.UNAUTHORIZED),
    TOKEN_PARSING_FAILURE("토큰 파싱에 실패했습니다.", HttpStatus.BAD_REQUEST)

    ;
    private final String message;
    private final HttpStatus httpStatus;
}
