package store.lastdance.domain.user;

import lombok.*;
import jakarta.persistence.*;
import store.lastdance.domain.admin.Report;
import store.lastdance.domain.admin.PenaltyType;
import store.lastdance.domain.common.BaseTimeEntity;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Entity
@Table(name = "user_penalties")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserPenalty extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "penalty_id")
    private Long penaltyId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "report_id", nullable = false)
    private Long reportId;

    @Column(name = "penalty_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private PenaltyType penaltyType;

    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(name = "duration_days")
    private Integer durationDays;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate = LocalDateTime.now();

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "admin_id", nullable = false)
    private UUID adminId;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id", insertable = false, updatable = false)
    private Report report;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id", insertable = false, updatable = false)
    private User admin;

    @Builder
    public UserPenalty(@NonNull UUID userId, @NonNull Long reportId, @NonNull PenaltyType penaltyType,
                       @NonNull String reason, @NonNull UUID adminId, Integer durationDays) {
        this.userId = userId;
        this.reportId = reportId;
        this.penaltyType = penaltyType;
        this.reason = reason;
        this.adminId = adminId;
        this.durationDays = durationDays;
        this.startDate = LocalDateTime.now();
        this.isActive = true;
        
        // 기간이 있는 경우 종료일 계산
        if (durationDays != null && durationDays > 0) {
            this.endDate = this.startDate.plusDays(durationDays);
        }
    }

    public void deactivate() {
        this.isActive = false;
    }

    public void activate() {
        this.isActive = true;
    }

    public boolean isExpired() {
        return this.endDate != null && LocalDateTime.now().isAfter(this.endDate);
    }
}
