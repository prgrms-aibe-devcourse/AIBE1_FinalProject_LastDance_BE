package store.lastdance.domain.calendar;

import lombok.Getter;

@Getter
public enum ExceptionType {
    DELETED("삭제됨"),           // 특정 날짜 삭제
    MODIFIED("수정됨"),          // 특정 날짜 내용 변경
    CANCELLED("취소됨"),         // 특정 날짜 취소
    MOVED("이동됨");            // 특정 날짜 시간 변경

    private final String description;

    ExceptionType(String description) {
        this.description = description;
    }

}