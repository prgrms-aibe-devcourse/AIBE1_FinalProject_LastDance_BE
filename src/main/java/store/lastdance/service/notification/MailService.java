package store.lastdance.service.notification;

public interface MailService {
    void sendSimpleMail(String to, String subject, String text);
}
