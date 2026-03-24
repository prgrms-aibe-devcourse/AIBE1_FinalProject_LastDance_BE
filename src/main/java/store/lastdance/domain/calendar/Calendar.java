package store.lastdance.domain.calendar;

import lombok.*;
import jakarta.persistence.*;
import store.lastdance.domain.common.BaseTimeEntity;
import java.time.LocalDateTime;
import store.lastdance.domain.group.Group;
import store.lastdance.domain.user.User;

@Getter
@Entity
@Table(
        name = "calendars",
        indexes = {
                @Index(name = "idx_calendar_user_date", columnList = "user_id,start_date,end_date"),
                @Index(name = "idx_calendar_group_date", columnList = "group_id,start_date,end_date")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Calendar extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "calendar_id")
    private Long calendarId;

    @Version
    @Column(name = "version")
    private Long version;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private Group group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "repeat_type", length = 20)
    @Enumerated(EnumType.STRING)
    private RepeatType repeatType = RepeatType.NONE;

    @Column(name = "repeat_end_date")
    private LocalDateTime repeatEndDate;

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

    public void updateCategory(CalendarCategory category) {
        this.category = category;
    }

    public void updateAsRepeating(RepeatType repeatType, LocalDateTime repeatEndDate) {
        this.repeatType = repeatType;
        this.repeatEndDate = repeatEndDate;
    }
    
    public void updateRepeatEndDate(LocalDateTime newRepeatEndDate) {
        this.repeatEndDate = newRepeatEndDate;
    }

    public void removeRepeat() {
        this.repeatType = RepeatType.NONE;
        this.repeatEndDate = null;
    }

    public static Calendar copyWithNewDate(Calendar base, LocalDateTime newStart, LocalDateTime newEnd) {
        return Calendar.builder()
                .calendarId(base.getCalendarId())
                .title(base.getTitle())
                .description(base.getDescription())
                .startDate(newStart)
                .endDate(newEnd)
                .isAllDay(base.getIsAllDay())
                .type(base.getType())
                .category(base.getCategory())
                .group(base.getGroup())
                .user(base.getUser())
                .repeatType(base.getRepeatType())
                .repeatEndDate(base.getRepeatEndDate())
                .build();
    }
}
