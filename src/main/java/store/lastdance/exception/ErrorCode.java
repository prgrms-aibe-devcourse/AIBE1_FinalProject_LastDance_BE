package store.lastdance.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import store.lastdance.dto.community.comment.CommentResponseDTO;

import java.util.UUID;

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
    TOKEN_NOT_FOUND("토큰이 없습니다.", HttpStatus.UNAUTHORIZED),
    TOKEN_TYPE_MISMATCH("토큰 타입이 일치하지 않습니다.", HttpStatus.UNAUTHORIZED),
    REFRESH_TOKEN_NOT_IN_REDIS("저장된 리프레시 토큰이 없습니다.", HttpStatus.UNAUTHORIZED),
    REFRESH_TOKEN_MISMATCH("리프레시 토큰이 일치하지 않습니다.", HttpStatus.UNAUTHORIZED),

    // 파일 업로드 관련
    FILE_UPLOAD_FAILED("파일 업로드에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    FILE_NOT_FOUND("파일을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    FILE_DELETE_FAILED("파일 삭제에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    EMPTY_FILE("빈 파일입니다.", HttpStatus.BAD_REQUEST),
    INVALID_FILE_TYPE("유효하지 않은 파일 형식입니다.", HttpStatus.BAD_REQUEST),
    INVALID_FILE_EXTENSION("유효하지 않은 파일 확장자입니다.", HttpStatus.BAD_REQUEST),
    FILE_SIZE_EXCEEDED("파일 크기를 초과했습니다.", HttpStatus.BAD_REQUEST),
    FILE_DOWNLOAD_FAILED("파일 다운로드 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),

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
    GROUP_MAX_MEMBERS_EXCEEDED("그룹 최대 인원 수를 초과했습니다.", HttpStatus.BAD_REQUEST),

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
    CHECKLIST_NOT_COMPLETED("완료되지 않은 체크리스트입니다.", HttpStatus.BAD_REQUEST),

    // 지출 관련
    EXPENSE_NOT_FOUND("지출내역을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    SPLIT_DATA_REQUIRED("정산 데이터가 필요합니다.", HttpStatus.BAD_REQUEST),
    EXPENSE_ACCESS_DENIED("지출내역에 대한 접근 권한이 없습니다.", HttpStatus.FORBIDDEN), // 새로 추가
    INVALID_CATEGORY("유효하지 않은 카테고리입니다.", HttpStatus.BAD_REQUEST),

    // 커뮤니티 관련
    COMMENT_NOT_FOUND("댓글을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    POST_NOT_FOUND("게시글을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),

    // 관리자 관련
    ADMIN_ACCESS_DENIED("관리자 권한이 없습니다.", HttpStatus.FORBIDDEN),
    INVALID_PERIOD("유효하지 않은 기간입니다.", HttpStatus.BAD_REQUEST),
    USER_ALREADY_BANNED("이미 정지된 사용자입니다.", HttpStatus.BAD_REQUEST),
    USER_NOT_BANNED("정지되지 않은 사용자입니다.", HttpStatus.BAD_REQUEST),
    REPORT_NOT_FOUND("존재하지 않는 신고 ID입니다.", HttpStatus.NOT_FOUND),
    AI_JUDGMENT_NOT_FOUND("존재하지 않는 AI 판단 ID입니다.", HttpStatus.NOT_FOUND),

    //LLM 관련
    JSON_PROCESSING_ERROR("JSON 처리 중 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    LLM_INVALID_RESPONSE("LLM 응답 처리 중 오류가 발생했습니다.",HttpStatus.INTERNAL_SERVER_ERROR),
    LLM_PARSING_FAILED("LLM 응답 파싱중 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    LLM_API_KEY_MISSING("LLM API 키가 설정되지 않았습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    TOO_MANY_REQUESTS("요청이 너무 잦습니다. 잠시 후 다시 시도해주세요.", HttpStatus.TOO_MANY_REQUESTS),
    HISTORY_NOT_FOUND("LLM 분석 기록을 찾을 수 없습니다.", HttpStatus.NOT_FOUND ),
    LLM_SERVICE_UNAVAILABLE("AI 분석 서비스가 일시적으로 응답하지 않습니다. 잠시 후 다시 시도해주세요.", HttpStatus.SERVICE_UNAVAILABLE)
    ,
    INVALID_HISTORY_REQUEST("LLM 분석 기록 요청이 유효하지 않습니다.", HttpStatus.BAD_REQUEST), PROMPT_NOT_FOUND("프롬프트를 찾을 수 없습니다", HttpStatus.NOT_FOUND )
    ,

    // 캘린더 관련
    CALENDAR_CREATE_FAILED("일정 생성에 실패했습니다", HttpStatus.INTERNAL_SERVER_ERROR),
    CALENDAR_FOUND_FAILED("일정 조회에 실패했습니다", HttpStatus.INTERNAL_SERVER_ERROR),
    CALENDAR_UPDATE_FAILED("일정 수정에 실패했습니다", HttpStatus.INTERNAL_SERVER_ERROR),
    CALENDAR_DELETE_FAILED("일정 삭제에 실패했습니다", HttpStatus.INTERNAL_SERVER_ERROR),
    CALENDAR_NOT_FOUND("일정을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    CALENDAR_ACCESS_DENIED("일정에 대한 접근 권한이 없습니다.", HttpStatus.FORBIDDEN),
    CALENDAR_INVALID_CATEGORY("유효하지 않은 카테고리입니다.", HttpStatus.BAD_REQUEST),
    CALENDAR_INVALID_TIME_ORDER("시작 시간이 종료 시간보다 늦을 수 없습니다.", HttpStatus.BAD_REQUEST),
    CALENDAR_END_DATE_WITHOUT_REPEAT("반복되지 않는 일정에 반복 종료일을 설정할 수 없습니다.", HttpStatus.BAD_REQUEST),
    CALENDAR_INVALID_REPEAT_DATE_ORDER("반복 종료일은 일정 시작일보다 이후여야 합니다.", HttpStatus.BAD_REQUEST),
    CALENDAR_REPEAT_REQUIRED("반복 타입은 필수입니다.", HttpStatus.BAD_REQUEST),
    CALENDAR_DATE_REQUIRED("시작날짜와 종료날짜를 입력해주세요.", HttpStatus.BAD_REQUEST),
    CALENDAR_ALREADY_PRESENT("해당 시간에 이미 그룹 일정이 있습니다", HttpStatus.CONFLICT);

    private final String message;
    private final HttpStatus httpStatus;
}
