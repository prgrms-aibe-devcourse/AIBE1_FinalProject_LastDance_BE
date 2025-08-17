package store.lastdance.domain.notification;

import lombok.*;
import jakarta.persistence.*;
import store.lastdance.domain.user.User;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notification_settings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationSetting {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "setting_id")
    private Long settingId;

    @Column(name = "user_id", unique = true, nullable = false)
    private UUID userId;

    @Column(name = "email_enabled")
    private Boolean emailEnabled = true;

    @Column(name = "schedule_reminder")
    private Boolean scheduleReminder = true;

    @Column(name = "payment_reminder")
    private Boolean paymentReminder = true;

    @Column(name = "checklist_reminder")
    private Boolean checklistReminder = true;

    // SSE 연결 상태
    @Column(name = "sse_enabled")
    private Boolean sseEnabled = true;
    

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @Builder
    public NotificationSetting(@NonNull UUID userId) {
        this.userId = userId;
        this.emailEnabled = true;
        this.scheduleReminder = true;
        this.paymentReminder = true;
        this.checklistReminder = true;
    }

    public void updateEmailEnabled(Boolean emailEnabled) {
        this.emailEnabled = emailEnabled;
    }

    public void updateScheduleReminder(Boolean scheduleReminder) {
        this.scheduleReminder = scheduleReminder;
    }

    public void updatePaymentReminder(Boolean paymentReminder) {
        this.paymentReminder = paymentReminder;
    }

    public void updateChecklistReminder(Boolean checklistReminder) {
        this.checklistReminder = checklistReminder;
    }

    

    public void updateSSEEnabled(Boolean sseEnabled) {
        this.sseEnabled = sseEnabled;
        // SSE 비활성화시 연결 상태도 초기화
        // 이 곳에 오프라인 처리가 필요하다면, 이벤트를 발행하거나
        // ApplicationContext를 통해 OnlineStatusService를 직접 호출해야 합니다.
        // 현재 구조에서는 엔티티가 서비스에 직접 의존하지 않는 것이 좋으므로,
        // 이 로직은 서비스를 사용하는 상위 계층으로 이동하는 것을 권장합니다.
    }

    // 유틸리티 메서드들
    public boolean isNotificationEnabled(NotificationType type) {
        return switch (type) {
            case SCHEDULE -> scheduleReminder != null && scheduleReminder;
            case PAYMENT -> paymentReminder != null && paymentReminder;
            case CHECKLIST -> checklistReminder != null && checklistReminder;
        };
    }

    public boolean isEmailEnabled() {
        return emailEnabled != null && emailEnabled;
    }

    public boolean isEmailEnabledForType(NotificationType type) {
        return isEmailEnabled() && isNotificationEnabled(type);
    }
}
