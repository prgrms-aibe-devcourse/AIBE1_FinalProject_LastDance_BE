package store.lastdance.service.notification;

public interface MailV2Service {
    void sendSimpleMail(String to, String subject, String text, String provider);

    void sendScheduleReminder(String to, String scheduleTitle, String message, String provider);

    void sendPaymentReminder(String to, String paymentTitle, String message, String provider);

    void sendChecklistReminder(String to, String checklistTitle, String message, String provider);
}
