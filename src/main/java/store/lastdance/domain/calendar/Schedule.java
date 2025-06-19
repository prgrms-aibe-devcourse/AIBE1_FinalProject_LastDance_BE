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
    public Schedule(@NonNull String title, @NonNull LocalDateTime startDate, @NonNull LocalDateTime endDate,
                    @NonNull ScheduleType type, @NonNull ScheduleCategory category, @NonNull UUID userId) {
        this.title = title;
        this.startDate = startDate;
        this.endDate = endDate;
        this.type = type;
        this.category = category;
        this.userId = userId;
        this.isAllDay = false;
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

    public void setAllDay(Boolean isAllDay) {
        this.isAllDay = isAllDay;
    }

    public void updateCategory(ScheduleCategory category) {
        this.category = category;
    }

    public void setRepeat(RepeatType repeatType, LocalDateTime repeatEndDate) {
        this.repeatType = repeatType;
        this.repeatEndDate = repeatEndDate;
    }

    public void removeRepeat() {
        this.repeatType = null;
        this.repeatEndDate = null;
    }

    public void setGroupId(UUID groupId) {
        this.groupId = groupId;
    }
}
