package store.lastdance.dto.calender.request;

import store.lastdance.domain.calendar.ScheduleCategory;
import store.lastdance.domain.calendar.ScheduleType;
import store.lastdance.domain.calendar.RepeatType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class CreateScheduleRequestDTO {

    @NotBlank(message = "제목은 필수입니다.")
    @Size(max = 200, message = "제목은 200자 이내로 입력해주세요.")
    private String title;

    @Size(max = 1000, message = "설명은 1000자 이내로 입력해주세요.")
    private String description;

    @NotNull(message = "시작 날짜는 필수입니다.")
    private LocalDateTime startDate;

    @NotNull(message = "종료 날짜는 필수입니다.")
    private LocalDateTime endDate;

    private Boolean isAllDay = false;

    @NotNull(message = "일정 타입은 필수입니다.")
    private ScheduleType type;

    @NotNull(message = "카테고리는 필수입니다.")
    private ScheduleCategory category;

    private UUID groupId;

    private RepeatType repeatType = RepeatType.NONE;

    private LocalDateTime repeatEndDate;
}