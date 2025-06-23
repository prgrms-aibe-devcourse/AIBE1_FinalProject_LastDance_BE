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
    UNSUPPORTED_OAUTH_PROVIDER("지원하지 않는 소셜 로그인 제공자입니다.", HttpStatus.BAD_REQUEST),
    NICKNAME_ALREADY_EXISTS("이미 사용 중인 닉네임입니다.", HttpStatus.CONFLICT),

    // 토큰 관련
    TOKEN_ERROR("토큰 에러", HttpStatus.UNAUTHORIZED),
    INVALID_TOKEN("유효하지 않은 토큰입니다.", HttpStatus.UNAUTHORIZED),
    INVALID_REFRESH_TOKEN("유효하지 않은 리프레시 토큰입니다.", HttpStatus.UNAUTHORIZED),
    EXPIRED_TOKEN("만료된 토큰입니다.", HttpStatus.UNAUTHORIZED),
    TOKEN_PARSING_FAILURE("토큰 파싱에 실패했습니다.", HttpStatus.BAD_REQUEST),

    // 파일 업로드 관련
    FILE_UPLOAD_FAILED("파일 업로드에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    FILE_NOT_FOUND("파일을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    FILE_DELETE_FAILED("파일 삭제에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    EMPTY_FILE("빈 파일입니다.", HttpStatus.BAD_REQUEST),
    INVALID_FILE_TYPE("유효하지 않은 파일 형식입니다.", HttpStatus.BAD_REQUEST),
    INVALID_FILE_EXTENSION("유효하지 않은 파일 확장자입니다.", HttpStatus.BAD_REQUEST),
    FILE_SIZE_EXCEEDED("파일 크기를 초과했습니다.", HttpStatus.BAD_REQUEST),
    FILE_DOWNLOAD_FAILED("파일 다운로드 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),

    ;
    private final String message;
    private final HttpStatus httpStatus;
}
