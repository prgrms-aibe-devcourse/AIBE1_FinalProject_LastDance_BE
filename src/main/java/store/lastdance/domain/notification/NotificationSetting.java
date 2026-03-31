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
    
    @Column(name = "email_enabled", nullable = false)
    private boolean emailEnabled = false;

    @Column(name = "schedule_reminder", nullable = false)
    private boolean scheduleReminder = false;

    @Column(name = "payment_reminder", nullable = false)
    private boolean paymentReminder = false;

    @Column(name = "checklist_reminder", nullable = false)
    private boolean checklistReminder = false;

    @Column(name = "sse_enabled", nullable = false)
    private boolean sseEnabled = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @Builder
    public NotificationSetting(@NonNull UUID userId) {
        this.userId = userId;
    }

    public void updateEmailEnabled(boolean emailEnabled) {
        this.emailEnabled = emailEnabled;
    }

    public void updateScheduleReminder(boolean scheduleReminder) {
        this.scheduleReminder = scheduleReminder;
    }

    public void updatePaymentReminder(boolean paymentReminder) {
        this.paymentReminder = paymentReminder;
    }

    public void updateChecklistReminder(boolean checklistReminder) {
        this.checklistReminder = checklistReminder;
    }

    public void updateSSEEnabled(boolean sseEnabled) { this.sseEnabled = sseEnabled; }

    public boolean isNotificationEnabled(NotificationType type) {
        return switch (type) {
            case SCHEDULE -> scheduleReminder;
            case PAYMENT -> paymentReminder;
            case CHECKLIST -> checklistReminder;
        };
    }

    public boolean isEmailEnabledForType(NotificationType type) {
        return isEmailEnabled() && isNotificationEnabled(type);
    }
}
