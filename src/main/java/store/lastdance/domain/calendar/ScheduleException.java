package store.lastdance.domain.calendar;

import lombok.*;
import jakarta.persistence.*;
import store.lastdance.domain.common.BaseTimeEntity;
import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "schedule_exceptions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ScheduleException extends BaseTimeEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "exception_id")
    private Long exceptionId;
    
    @Column(name = "schedule_id", nullable = false)
    private Long scheduleId;
    
    @Column(name = "exception_date", nullable = false)
    private LocalDateTime exceptionDate;
    
    @Column(name = "exception_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ExceptionType exceptionType;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", insertable = false, updatable = false)
    private Schedule schedule;
    
    @Builder
    public ScheduleException(@NonNull Long scheduleId, 
                            @NonNull LocalDateTime exceptionDate, 
                            @NonNull ExceptionType exceptionType) {
        this.scheduleId = scheduleId;
        this.exceptionDate = exceptionDate;
        this.exceptionType = exceptionType;
    }
}