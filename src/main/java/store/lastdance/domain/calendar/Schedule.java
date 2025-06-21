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
@Table(name = "schedules")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Schedule extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "schedule_id")
    private Long scheduleId;

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
    private ScheduleType type;

    @Column(name = "category", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ScheduleCategory category;

    @Column(name = "group_id")
    private UUID groupId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "repeat_type", length = 20)
    @Enumerated(EnumType.STRING)
    private RepeatType repeatType;

    @Column(name = "repeat_end_date")
    private LocalDateTime repeatEndDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", insertable = false, updatable = false)
    private Group group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @Builder
    public Schedule(@NonNull String title,
                    @NonNull String description,
                   @NonNull LocalDateTime startDate,
                   @NonNull LocalDateTime endDate,
                   Boolean isAllDay,
                   @NonNull ScheduleType type,
                   @NonNull ScheduleCategory category,
                   UUID groupId,
                   @NonNull UUID userId,
                   RepeatType repeatType,
                   LocalDateTime repeatEndDate) {
        this.title = title;
        this.description = description;
        this.startDate = startDate;
        this.endDate = endDate;
        this.isAllDay = isAllDay != null ? isAllDay : false;
        this.type = type;
        this.category = category;
        this.groupId = groupId;
        this.userId = userId;
        this.repeatType = repeatType != null ? repeatType : RepeatType.NONE;
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

    public void updateType(ScheduleType type) {
        this.type = type;
    }

    public void updateCategory(ScheduleCategory category) {
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
    
    /**
     * 특정 날짜 이후의 반복 일정을 중단
     */
    public void stopRepeatingAfter(LocalDateTime cutoffDate) {
        if (this.repeatType == null || this.repeatType == RepeatType.NONE) {
            throw new IllegalArgumentException("반복되지 않는 일정은 중단할 수 없습니다.");
        }
        
        LocalDateTime newEndDate = cutoffDate.minusDays(1);
        if (newEndDate.isBefore(this.startDate)) {
            // 새로운 종료일이 시작일보다 이르면 반복을 완전히 제거
            removeRepeat();
        } else {
            this.repeatEndDate = newEndDate;
        }
    }
    
    public void removeRepeat() {
        this.repeatType = RepeatType.NONE;
        this.repeatEndDate = null;
    }
    
    public void makeWeeklyRepeat(LocalDateTime untilDate) {
        setAsRepeating(RepeatType.WEEKLY, untilDate);
    }
    
    public void makeMonthlyRepeat(LocalDateTime untilDate) {
        setAsRepeating(RepeatType.MONTHLY, untilDate);
    }
    
    public void makeDailyRepeat(LocalDateTime untilDate) {
        setAsRepeating(RepeatType.DAILY, untilDate);
    }
    
    // 검증 메서드들
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

    public void updateGroupId(UUID groupId) {
        this.groupId = groupId;
    }
}
