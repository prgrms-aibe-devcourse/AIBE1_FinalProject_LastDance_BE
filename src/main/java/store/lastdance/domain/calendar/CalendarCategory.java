package store.lastdance.domain.calendar;

import lombok.Getter;

@Getter
public enum CalendarCategory {
    GENERAL("일반"),
    PAYMENT("청구서/결제"),
    HOUSEHOLD("청소"),
    MEETING("회의"),
    APPOINTMENT("약속"),
    HEALTH("건강"),
    SHOPPING("쇼핑"),
    TRAVEL("여행"),
    OTHER("기타");

    private final String description;

    CalendarCategory(String description) {
        this.description = description;
    }

}