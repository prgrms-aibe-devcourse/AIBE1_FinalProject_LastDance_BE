package store.lastdance.dto.calendar.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import store.lastdance.domain.calendar.CalendarCategory;
import store.lastdance.domain.calendar.RepeatType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCalendarRequestDTO {

    @NotBlank(message = "제목은 필수입니다.")
    @Size(max = 200, message = "제목은 200자 이내로 입력해주세요.")
    private String title;

    @Size(max = 1000, message = "설명은 1000자 이내로 입력해주세요.")
    private String description;

    @NotNull(message = "시작일은 필수입니다.")
    private LocalDateTime startDate;

    @NotNull(message = "종료일은 필수입니다.")
    private LocalDateTime endDate;

    @NotNull(message = "종일 여부는 필수입니다.")
    private Boolean isAllDay;

    @NotNull(message = "카테고리는 필수입니다.")
    private CalendarCategory category;

    private RepeatType repeatType = RepeatType.NONE;

    private LocalDateTime repeatEndDate;
}