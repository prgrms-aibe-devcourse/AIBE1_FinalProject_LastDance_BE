package store.lastdance.dto.calender.response;

import store.lastdance.domain.calendar.Calendar;
import store.lastdance.domain.calendar.CalendarCategory;
import store.lastdance.domain.calendar.CalendarType;
import store.lastdance.domain.calendar.RepeatType;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class CalendarResponseDTO {

    private Long calendarId;
    private String title;
    private String description;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Boolean isAllDay;
    private CalendarType type;
    private CalendarCategory category;
    private UUID groupId;
    private String groupName;
    private UUID userId;
    private RepeatType repeatType;
    private LocalDateTime repeatEndDate;
    private LocalDateTime createdAt;
    private List<LocalDateTime> exceptionDates;

    public static CalendarResponseDTO from(Calendar calendar, String groupName) {
        CalendarResponseDTOBuilder calendarResponseDTOBuilder = CalendarResponseDTO.builder()
                .calendarId(calendar.getCalendarId())
                .title(calendar.getTitle())
                .description(calendar.getDescription())
                .startDate(calendar.getStartDate())
                .endDate(calendar.getEndDate())
                .isAllDay(calendar.getIsAllDay())
                .type(calendar.getType())
                .category(calendar.getCategory())
                .groupId(calendar.getGroupId())
                .groupName(groupName)
                .userId(calendar.getUserId())
                .repeatType(calendar.getRepeatType())
                .repeatEndDate(calendar.getRepeatEndDate())
                .createdAt(calendar.getCreatedAt())
                .exceptionDates(null);

        return calendarResponseDTOBuilder.build();
    }

    public static CalendarResponseDTO from(Calendar calendar) {
        return from(calendar, null);
    }
}