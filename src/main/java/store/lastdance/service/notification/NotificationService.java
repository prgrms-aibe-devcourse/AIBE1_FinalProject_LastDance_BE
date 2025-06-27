package store.lastdance.service.notification;

import store.lastdance.domain.calendar.Calendar;
import store.lastdance.domain.notification.NotificationCache;

import java.util.List;
import java.util.UUID;

public interface NotificationService {
    // 일정 알림
    void sendEmailScheduleReminder(Calendar calendar);
    boolean isAlreadyNotified(Long calendarId, UUID userId);
    
    // 할일 알림 (추후 구현)
    // void sendEmailChecklistReminder(Checklist checklist);
    // boolean isChecklistAlreadyNotified(Long checklistId, UUID userId);
    
    // 지출 알림 (추후 구현)  
    // void sendEmailPaymentReminder(Payment payment);
    // boolean isPaymentAlreadyNotified(Long paymentId, UUID userId);
    
    // 알림 조회 기능
    List<NotificationCache> getUserNotifications(UUID userId);
    List<NotificationCache> getUserNotificationsByType(UUID userId, String type);
}
