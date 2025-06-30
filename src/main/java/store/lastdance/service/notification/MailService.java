package store.lastdance.service.notification;

public interface MailService {
    /**
     * 단순 텍스트 메일 발송 (기본 메일 서비스 사용)
     */
    void sendSimpleMail(String to, String subject, String text);
    
    /**
     * 지정된 메일 서비스로 단순 텍스트 메일 발송
     */
    void sendSimpleMail(String to, String subject, String text, String provider);
    
    /**
     * 일정 알림 메일 발송
     */
    void sendScheduleReminder(String to, String scheduleTitle, String message);
    
    /**
     * 일정 알림 메일 발송 (특정 메일 서비스 사용)
     */
    void sendScheduleReminder(String to, String scheduleTitle, String message, String provider);
    
    /**
     * 정산 요청 알림 메일 발송
     */
    void sendPaymentReminder(String to, String paymentTitle, String message);
    
    /**
     * 정산 요청 알림 메일 발송 (특정 메일 서비스 사용)
     */
    void sendPaymentReminder(String to, String paymentTitle, String message, String provider);
    
    /**
     * 체크리스트 알림 메일 발송
     */
    void sendChecklistReminder(String to, String checklistTitle, String message);
    
    /**
     * 체크리스트 알림 메일 발송 (특정 메일 서비스 사용)
     */
    void sendChecklistReminder(String to, String checklistTitle, String message, String provider);
    
    /**
     * 사용 가능한 메일 서비스 확인
     */
    boolean isProviderAvailable(String provider);
}
