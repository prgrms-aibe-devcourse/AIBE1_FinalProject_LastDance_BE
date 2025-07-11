package store.lastdance.domain.calendar;

import lombok.*;
import jakarta.persistence.*;
import store.lastdance.domain.group.Group;
import store.lastdance.domain.user.User;
import store.lastdance.domain.common.BaseTimeEntity;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Entity
@Table(name = "calendars")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Calendar extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "calendar_id")
    private Long calendarId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    @Column(name = "is_all_day", nullable = false)
    private Boolean isAllDay = false;

    @Column(name = "type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private CalendarType type;

    @Column(name = "category", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private CalendarCategory category;

    @Column(name = "group_id")
    private UUID groupId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "repeat_type", length = 20)
    @Enumerated(EnumType.STRING)
    private RepeatType repeatType = RepeatType.NONE;

    @Column(name = "repeat_end_date")
    private LocalDateTime repeatEndDate;

    @Builder
    public Calendar(@NonNull String title,
                    @NonNull String description,
                   @NonNull LocalDateTime startDate,
                   @NonNull LocalDateTime endDate,
                   Boolean isAllDay,
                   @NonNull CalendarType type,
                   @NonNull CalendarCategory category,
                   UUID groupId,
                   @NonNull UUID userId,
                   RepeatType repeatType,
                   LocalDateTime repeatEndDate) {
        this.title = title;
        this.description = description;
        this.startDate = startDate;
        this.endDate = endDate;
        this.isAllDay = isAllDay;
        this.type = type;
        this.category = category;
        this.groupId = groupId;
        this.userId = userId;
        this.repeatType = repeatType;
        this.repeatEndDate = repeatEndDate;
    }

    public void updateTitle(String title) {
        this.title = title;
    }

    public void updateDescription(String description) {
        this.description = description;
    }

    public void updateDateTime(LocalDateTime startDate, LocalDateTime endDate) {
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public void updateAllDay(Boolean isAllDay) {
        this.isAllDay = isAllDay != null ? isAllDay : false;
    }

    public void updateType(CalendarType type) {
        this.type = type;
    }

    public void updateCategory(CalendarCategory category) {
        this.category = category;
    }

    public void setAsRepeating(RepeatType repeatType, LocalDateTime repeatEndDate) {
        validateRepeatSettings(repeatType, repeatEndDate);
        this.repeatType = repeatType;
        this.repeatEndDate = repeatEndDate;
    }
    
    public void updateRepeatEndDate(LocalDateTime newRepeatEndDate) {
        if (this.repeatType == null || this.repeatType == RepeatType.NONE) {
            throw new IllegalArgumentException("반복되지 않는 일정의 반복 종료일은 설정할 수 없습니다.");
        }
        validateRepeatEndDate(newRepeatEndDate);
        this.repeatEndDate = newRepeatEndDate;
    }

    public void updateGroupId(UUID groupId) {
        this.groupId = groupId;
    }

    public void removeRepeat() {
        this.repeatType = RepeatType.NONE;
        this.repeatEndDate = null;
    }

    private void validateRepeatSettings(RepeatType repeatType, LocalDateTime repeatEndDate) {
        if (repeatType == null) {
            throw new IllegalArgumentException("반복 타입은 필수입니다.");
        }

        if (repeatType != RepeatType.NONE && repeatEndDate != null) {
            validateRepeatEndDate(repeatEndDate);
        }
    }

    private void validateRepeatEndDate(LocalDateTime repeatEndDate) {
        if (repeatEndDate != null && repeatEndDate.isBefore(this.startDate)) {
            throw new IllegalArgumentException("반복 종료일은 일정 시작일보다 이후여야 합니다.");
        }
    }

}
