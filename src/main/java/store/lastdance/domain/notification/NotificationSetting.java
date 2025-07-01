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

    // 웹푸시 관련
    @Column(name = "webpush_enabled")
    private Boolean webpushEnabled = true;
    @Column(name = "webpush_endpoint")
    private String webpushEndpoint;
    @Column(name = "webpushP256dh")
    private String webpushP256dh;
    @Column(name = "webpush_auth")
    private String webpushAuth;

    // SSE 연결 상태
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

    // 웹푸시 관련 메서드들
    public void updateWebPushEnabled(Boolean webpushEnabled) {
        this.webpushEnabled = webpushEnabled;
    }

    public void updateWebPushSubscription(String endpoint, String p256dh, String auth) {
        this.webpushEndpoint = endpoint;
        this.webpushP256dh = p256dh;
        this.webpushAuth = auth;
        this.webpushEnabled = true; // 구독 등록시 자동 활성화
    }

    public void removeWebPushSubscription() {
        this.webpushEndpoint = null;
        this.webpushP256dh = null;
        this.webpushAuth = null;
        this.webpushEnabled = false;
    }

    // SSE 연결 상태 관리
    public void updateOnlineStatus(Boolean isOnline) {
        this.isOnline = isOnline;
        this.lastSeen = LocalDateTime.now();
    }

    // 유틸리티 메서드들
    public boolean isNotificationEnabled(NotificationType type) {
        return switch (type) {
            case SCHEDULE -> scheduleReminder;
            case PAYMENT -> paymentReminder;
            case CHECKLIST -> checklistReminder;
        };
    }

    public boolean hasWebPushSubscription() {
        return webpushEndpoint != null && webpushP256dh != null && webpushAuth != null;
    }

    public boolean isWebPushAvailable() {
        return webpushEnabled && hasWebPushSubscription();
    }
}
