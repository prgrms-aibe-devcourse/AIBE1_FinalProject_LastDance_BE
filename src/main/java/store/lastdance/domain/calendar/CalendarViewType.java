package store.lastdance.domain.calendar;

import com.fasterxml.jackson.annotation.JsonCreator;
import store.lastdance.exception.CustomException;
import store.lastdance.exception.ErrorCode;

public enum CalendarViewType {
    DAILY, WEEKLY, MONTHLY, YEARLY;

    @JsonCreator
    public static CalendarViewType from(String value) {
        if (value == null) {
            return MONTHLY;
        }
        try {
            return CalendarViewType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new CustomException(ErrorCode.CALENDAR_INVALID_VIEW_TYPE);
        }
    }
}
