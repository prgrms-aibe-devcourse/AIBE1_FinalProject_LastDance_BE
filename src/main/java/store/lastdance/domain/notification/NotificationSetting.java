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
    @Column(name = "is_online")
    private Boolean isOnline = false;
    @Column(name = "last_seen")
    private LocalDateTime lastSeen;

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

    // SSE 연결 상태 관리
    public void updateOnlineStatus(Boolean isOnline) {
        this.isOnline = isOnline;
        this.lastSeen = LocalDateTime.now();
    }

    public void updateSSEEnabled(Boolean sseEnabled) {
        this.sseEnabled = sseEnabled;
        // SSE 비활성화시 연결 상태도 초기화
        if (!sseEnabled) {
            this.isOnline = false;
        }
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
