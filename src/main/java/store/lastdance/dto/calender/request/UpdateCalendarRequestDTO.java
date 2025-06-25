package store.lastdance.dto.calender.request;

import store.lastdance.domain.calendar.CalendarCategory;
import store.lastdance.domain.calendar.CalendarType;
import store.lastdance.domain.calendar.RepeatType;

import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class UpdateCalendarRequestDTO {

    @Size(max = 200, message = "제목은 200자 이내로 입력해주세요.")
    private String title;

    @Size(max = 1000, message = "설명은 1000자 이내로 입력해주세요.")
    private String description;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    private Boolean isAllDay;

    private CalendarCategory category;

    private UUID groupId;

    private RepeatType repeatType;

    private LocalDateTime repeatEndDate;
}