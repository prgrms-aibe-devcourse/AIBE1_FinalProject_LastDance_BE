package store.lastdance.validation.calendar;

import java.util.UUID;
import store.lastdance.domain.calendar.Calendar;
import store.lastdance.domain.calendar.CalendarType;
import store.lastdance.domain.calendar.RepeatType;
import store.lastdance.dto.calender.request.CreateCalendarRequestDTO;
import store.lastdance.dto.calender.request.UpdateCalendarRequestDTO;
import store.lastdance.exception.CustomException;
import store.lastdance.exception.ErrorCode;

import java.time.LocalDateTime;

public final class CalendarValidator {

    public static void validateCreateCalendar(CreateCalendarRequestDTO request, UUID groupId) {
        validateCalendarCreation(request, groupId);
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

    /**
     * 반복 종료일 검증
     */
    public static void validateRepeatEndDate(LocalDateTime repeatEndDate, LocalDateTime startDate) {
        if (repeatEndDate == null || startDate == null) {
            throw new CustomException(ErrorCode.CALENDAR_DATE_REQUIRED);
        }

        if (repeatEndDate.isBefore(startDate)) {
            throw new CustomException(ErrorCode.CALENDAR_INVALID_REPEAT_DATE_ORDER);
        }
    }

    /**
     * 반복 타입과 반복 종료일 관계 검증
     */
    public static void validateRepeatType(RepeatType repeatType, LocalDateTime repeatEndDate) {
        if ((repeatType == null || repeatType == RepeatType.NONE) && repeatEndDate != null) {
            throw new CustomException(ErrorCode.CALENDAR_END_DATE_WITHOUT_REPEAT);
        }
    }

    public static void validateCalendarCreation(CreateCalendarRequestDTO request, UUID groupId) {
        validateDateOrder(request.getStartDate(), request.getEndDate());

        if (request.getRepeatType() != null) {
            validateRepeatSettings(request.getRepeatType(), request.getRepeatEndDate(), request.getStartDate());
        }
    }

    /**
     * 일정 수정 요청 검증
     */
    public static void validateCalendarUpdate(Calendar existing, UpdateCalendarRequestDTO request) {
        // 시작일/종료일이 모두 제공된 경우
        if (request.getStartDate() != null && request.getEndDate() != null) {
            validateDateOrder(request.getStartDate(), request.getEndDate());
        }
        // 시작일만 제공된 경우
        else if (request.getStartDate() != null) {
            validateDateOrder(request.getStartDate(), existing.getEndDate());
        }
        // 종료일만 제공된 경우
        else if (request.getEndDate() != null) {
            validateDateOrder(existing.getStartDate(), request.getEndDate());
        }

        // 반복 설정 검증
        if (request.getRepeatType() != null) {
            LocalDateTime startDate = request.getStartDate() != null ?
                request.getStartDate() : existing.getStartDate();

            validateRepeatSettings(request.getRepeatType(), request.getRepeatEndDate(), startDate);
        }
        // 반복 종료일만 수정하는 경우
        else if (request.getRepeatEndDate() != null) {
            validateRepeatType(existing.getRepeatType(), request.getRepeatEndDate());

            LocalDateTime startDate = request.getStartDate() != null ?
                request.getStartDate() : existing.getStartDate();
            validateRepeatEndDate(request.getRepeatEndDate(), startDate);
        }
    }

    // === 비즈니스 규칙 검증 메서드들 ===

    public static void validateGroupMembership(UUID groupId, boolean isMember) {
        if (groupId != null && !isMember) {
            throw new CustomException(ErrorCode.GROUP_ACCESS_DENIED);
        }
    }

    /**
     * 일정 접근 권한 검증
     */
    public static void validateCalendarAccess(Calendar calendar, UUID userId, boolean isGroupMember) {
        // 개인 일정인 경우 작성자만 접근 가능
        if (calendar.getType() == CalendarType.PERSONAL) {
            if (!calendar.getUser().getUserId().equals(userId)) {
                throw new CustomException(ErrorCode.CALENDAR_ACCESS_DENIED);
            }
        }
        // 그룹 일정인 경우 그룹 멤버만 접근 가능
        else if (calendar.getType() == CalendarType.GROUP) {
            if (!calendar.getUser().getUserId().equals(userId) && !isGroupMember) {
                throw new CustomException(ErrorCode.CALENDAR_ACCESS_DENIED);
            }
        }
    }

    /**
     * 일정 수정 권한 검증
     */
    public static void validateCalendarModification(Calendar calendar, UUID userId, boolean isGroupMember) {
        switch (calendar.getType()) {
            case PERSONAL -> {
                // 개인 일정: 작성자만 수정 가능
                if (!calendar.getUser().getUserId().equals(userId)) {
                    throw new CustomException(ErrorCode.CALENDAR_ACCESS_DENIED);
                }
            }
            case GROUP -> {
                // 그룹 일정: 작성자 또는 그룹 멤버가 수정 가능
                if (!calendar.getUser().getUserId().equals(userId) &&
                    (calendar.getGroup().getGroupId() == null || !isGroupMember)) {
                    throw new CustomException(ErrorCode.CALENDAR_ACCESS_DENIED);
                }
            }
        }
    }
}
