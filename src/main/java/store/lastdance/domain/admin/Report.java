package store.lastdance.domain.admin;

import lombok.*;
import jakarta.persistence.*;
import store.lastdance.domain.user.User;
import store.lastdance.domain.common.BaseTimeEntity;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Entity
@Table(name = "reports")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Report extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id")
    private Long reportId;

    @Column(name = "reporter_id", nullable = false)
    private UUID reporterId;

    @Column(name = "reported_user_id", nullable = false)
    private UUID reportedUserId;

    @Column(name = "report_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ReportType reportType;

    @Column(name = "target_id", nullable = false)
    private UUID targetId;

    @Column(name = "reason", nullable = false, length = 300)
    private String reason;

    @Column(name = "status", length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private ReportStatus status = ReportStatus.PENDING;

    @Column(name = "admin_id")
    private UUID adminId;

    @Column(name = "admin_comment", columnDefinition = "TEXT")
    private String adminComment;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", insertable = false, updatable = false)
    private User reporter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_user_id", insertable = false, updatable = false)
    private User reportedUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id", insertable = false, updatable = false)
    private User admin;

    @Builder
    public Report(@NonNull UUID reporterId, @NonNull UUID reportedUserId, @NonNull ReportType reportType,
                  @NonNull UUID targetId, @NonNull String reason) {
        this.reporterId = reporterId;
        this.reportedUserId = reportedUserId;
        this.reportType = reportType;
        this.targetId = targetId;
        this.reason = reason;
        this.status = ReportStatus.PENDING;
    }

    public void processReport(UUID adminId, String adminComment, ReportStatus status) {
        this.adminId = adminId;
        this.adminComment = adminComment;
        this.status = status;
        this.processedAt = LocalDateTime.now();
    }

    public void updateStatus(String status) {
        try {
            this.status = ReportStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid report status: " + status);
        }
    }
}
