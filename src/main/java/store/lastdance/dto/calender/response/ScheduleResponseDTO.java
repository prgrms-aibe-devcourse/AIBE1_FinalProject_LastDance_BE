package store.lastdance.dto.calender.response;

import store.lastdance.domain.calendar.Schedule;
import store.lastdance.domain.calendar.ScheduleCategory;
import store.lastdance.domain.calendar.ScheduleType;
import store.lastdance.domain.calendar.RepeatType;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class ScheduleResponseDTO {

    private Long scheduleId;
    private String title;
    private String description;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Boolean isAllDay;
    private ScheduleType type;
    private ScheduleCategory category;
    private UUID groupId;
    private UUID userId;
    private RepeatType repeatType;
    private LocalDateTime repeatEndDate;
    private LocalDateTime createdAt;

    // 연관 객체 정보
    private GroupInfo group;
    private UserInfo user;

    @Getter
    @Builder
    public static class GroupInfo {
        private UUID groupId;
        private String name;
        private String description;
    }

    @Getter
    @Builder
    public static class UserInfo {
        private UUID userId;
        private String name;
        private String email;
    }

    public static ScheduleResponseDTO from(Schedule schedule) {
        ScheduleResponseDTOBuilder scheduleResponseDTOBuilder = ScheduleResponseDTO.builder()
                .scheduleId(schedule.getScheduleId())
                .title(schedule.getTitle())
                .description(schedule.getDescription())
                .startDate(schedule.getStartDate())
                .endDate(schedule.getEndDate())
                .isAllDay(schedule.getIsAllDay())
                .type(schedule.getType())
                .category(schedule.getCategory())
                .groupId(schedule.getGroupId())
                .userId(schedule.getUserId())
                .repeatType(schedule.getRepeatType())
                .repeatEndDate(schedule.getRepeatEndDate())
                .createdAt(schedule.getCreatedAt());

        return scheduleResponseDTOBuilder.build();
    }
}