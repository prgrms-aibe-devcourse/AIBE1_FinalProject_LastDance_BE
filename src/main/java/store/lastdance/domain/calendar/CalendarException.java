package store.lastdance.domain.calendar;

import lombok.*;
import jakarta.persistence.*;
import store.lastdance.domain.common.BaseTimeEntity;
import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "calendar_exceptions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CalendarException extends BaseTimeEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "exception_id")
    private Long exceptionId;
    
    @Column(name = "calendar_id", nullable = false)
    private Long calendarId;
    
    @Column(name = "exception_date", nullable = false)
    private LocalDateTime exceptionDate;
    
    @Builder
    public CalendarException(@NonNull Long calendarId,
                             @NonNull LocalDateTime exceptionDate) {
        this.calendarId = calendarId;
        this.exceptionDate = exceptionDate;
    }
}