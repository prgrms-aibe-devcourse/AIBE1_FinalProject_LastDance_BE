package store.lastdance.service.notification;

public interface MailService {
    /**
     * 단순 텍스트 메일 발송
     */
    void sendSimpleMail(String to, String subject, String text);
    
    /**
     * 일정 알림 메일 발송
     */
    void sendScheduleReminder(String to, String scheduleTitle, String message);
    
    /**
     * 납부일 알림 메일 발송
     */
    void sendPaymentReminder(String to, String paymentTitle, String message);
    
    /**
     * 체크리스트 알림 메일 발송
     */
    void sendChecklistReminder(String to, String checklistTitle, String message);
}
