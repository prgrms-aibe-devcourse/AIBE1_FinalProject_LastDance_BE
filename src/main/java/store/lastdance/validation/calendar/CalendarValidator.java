package store.lastdance.validation.calendar;

import java.util.UUID;
import store.lastdance.domain.calendar.RepeatType;
import store.lastdance.dto.calendar.request.CreateCalendarRequestDTO;
import store.lastdance.exception.CustomException;
import store.lastdance.exception.ErrorCode;

import java.time.LocalDateTime;

public final class CalendarValidator {

    public static void validateCreateCalendar(CreateCalendarRequestDTO request, UUID groupId) {
        validateCalendarCreation(request, groupId);
        validateCalendarRequest(request);
    }

    public static void validateCalendarRequest(CreateCalendarRequestDTO request) {
        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new CustomException(ErrorCode.CALENDAR_INVALID_TIME_ORDER);
        }

        if (request.getRepeatType() != RepeatType.NONE && request.getRepeatEndDate() != null) {
            if (request.getRepeatEndDate().isBefore(request.getStartDate())) {
                throw new CustomException(ErrorCode.CALENDAR_INVALID_REPEAT_DATE_ORDER);
            }
        }
    }

    public static void validateDateOrder(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate == null || endDate == null) {
            throw new CustomException(ErrorCode.INVALID_CHECKLIST_REQUEST);
        }

        if (startDate.isAfter(endDate)) {
            throw new CustomException(ErrorCode.CALENDAR_INVALID_TIME_ORDER);
        }
    }

    public static void validateRepeatSettings(RepeatType repeatType, LocalDateTime repeatEndDate, LocalDateTime startDate) {
        if (repeatType == null) {
            throw new CustomException(ErrorCode.CALENDAR_REPEAT_REQUIRED);
        }

        if (repeatType != RepeatType.NONE && repeatEndDate != null) {
            validateRepeatEndDate(repeatEndDate, startDate);
        }
    }

    public static void validateRepeatEndDate(LocalDateTime repeatEndDate, LocalDateTime startDate) {
        if (repeatEndDate == null || startDate == null) {
            throw new CustomException(ErrorCode.CALENDAR_DATE_REQUIRED);
        }

        if (repeatEndDate.isBefore(startDate)) {
            throw new CustomException(ErrorCode.CALENDAR_INVALID_REPEAT_DATE_ORDER);
        }
    }

    public static void validateCalendarCreation(CreateCalendarRequestDTO request, UUID groupId) {
        validateDateOrder(request.getStartDate(), request.getEndDate());

        if (request.getRepeatType() != null) {
            validateRepeatSettings(request.getRepeatType(), request.getRepeatEndDate(), request.getStartDate());
        }
    }

    public static void validateGroupMembership(UUID groupId, boolean isMember) {
        if (groupId != null && !isMember) {
            throw new CustomException(ErrorCode.GROUP_ACCESS_DENIED);
        }
    }
}
