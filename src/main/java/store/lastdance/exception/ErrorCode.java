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
    TOKEN_PARSING_FAILURE("토큰 파싱에 실패했습니다.", HttpStatus.BAD_REQUEST),

    // 그룹 관련
    GROUP_NOT_FOUND("그룹을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    GROUP_ACCESS_DENIED("그룹에 대한 접근 권한이 없습니다.", HttpStatus.FORBIDDEN),
    GROUP_CREATION_FAILED("그룹 생성에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_GROUP_REQUEST("잘못된 그룹 요청입니다.", HttpStatus.BAD_REQUEST),
    GROUP_MEMBER_NOT_FOUND("그룹 멤버를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    INVALID_INVITE_CODE("유효하지 않은 초대 코드입니다.", HttpStatus.BAD_REQUEST),
    GROUP_OPERATION_FAILED("그룹 작업에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    ALREADY_APPLIED_GROUP("이미 그룹에 신청한 상태입니다.", HttpStatus.BAD_REQUEST),
    GROUP_OWNER_CANNOT_LEAVE("그룹 소유자는 그룹을 떠날 수 없습니다.", HttpStatus.BAD_REQUEST),

    // 체크리스트 관련
    CHECKLIST_NOT_FOUND("체크리스트를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    CHECKLIST_ACCESS_DENIED("체크리스트에 대한 접근 권한이 없습니다.", HttpStatus.FORBIDDEN),
    CHECKLIST_CREATION_FAILED("체크리스트 생성에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_CHECKLIST_REQUEST("잘못된 체크리스트 요청입니다.", HttpStatus.BAD_REQUEST),
    CHECKLIST_TITLE_REQUIRED("체크리스트 제목은 필수입니다.", HttpStatus.BAD_REQUEST),
    CHECKLIST_ASSIGNEE_REQUIRED("담당자 지정은 필수입니다.", HttpStatus.BAD_REQUEST),
    CHECKLIST_DUE_DATE_REQUIRED("마감일 설정은 필수입니다.", HttpStatus.BAD_REQUEST),
    CHECKLIST_PRIORITY_REQUIRED("우선순위 설정은 필수입니다.", HttpStatus.BAD_REQUEST),
    CHECKLIST_ALREADY_COMPLETED("이미 완료된 체크리스트입니다.", HttpStatus.BAD_REQUEST),
    CHECKLIST_NOT_COMPLETED("완료되지 않은 체크리스트입니다.", HttpStatus.BAD_REQUEST);

    private final String message;
    private final HttpStatus httpStatus;
}
