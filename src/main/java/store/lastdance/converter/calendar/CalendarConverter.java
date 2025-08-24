package store.lastdance.converter.calendar;

import org.springframework.stereotype.Component;
import store.lastdance.domain.calendar.Calendar;
import store.lastdance.domain.calendar.CalendarType;
import store.lastdance.domain.group.Group;
import store.lastdance.domain.user.User;
import store.lastdance.dto.calender.request.CreateCalendarRequestDTO;
import store.lastdance.dto.calender.response.CalendarResponseDTO;

import java.time.Duration;
import java.time.LocalDateTime;

@Component
public class CalendarConverter {
    public Calendar toEntity(CreateCalendarRequestDTO request, User user, Group group, CalendarType type) {
        return Calendar.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .isAllDay(request.getIsAllDay())
                .type(type)
                .category(request.getCategory())
                .group(group)
                .user(user)
                .repeatType(request.getRepeatType())
                .repeatEndDate(request.getRepeatEndDate())
                .build();
    }

    public Calendar toEntity(Calendar base, LocalDateTime newStartDate, Duration duration) {
        return Calendar.builder()
                .title(base.getTitle())
                .description(base.getDescription())
                .startDate(newStartDate)
                .endDate(newStartDate.plus(duration))
                .isAllDay(base.getIsAllDay())
                .type(base.getType())
                .category(base.getCategory())
                .group(base.getGroup())
                .user(base.getUser())
                .repeatType(base.getRepeatType())
                .repeatEndDate(base.getRepeatEndDate())
                .build();
    }

    public CalendarResponseDTO toDto(Calendar calendar, User user, Group group, String groupName) {
        return CalendarResponseDTO.builder()
                .calendarId(calendar.getCalendarId())
                .title(calendar.getTitle())
                .description(calendar.getDescription())
                .startDate(calendar.getStartDate())
                .endDate(calendar.getEndDate())
                .isAllDay(calendar.getIsAllDay())
                .type(calendar.getType())
                .category(calendar.getCategory())
                .groupId(calendar.getGroup() != null ? calendar.getGroup().getGroupId() : null)
                .groupName(groupName)
                .userId(calendar.getUser() != null ? calendar.getUser().getUserId() : null)
                .repeatType(calendar.getRepeatType())
                .repeatEndDate(calendar.getRepeatEndDate())
                .createdAt(calendar.getCreatedAt())
                .build();
    }
}
