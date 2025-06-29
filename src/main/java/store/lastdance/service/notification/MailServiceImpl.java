package store.lastdance.service.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailServiceImpl implements MailService {

    private final JavaMailSender mailSender;
    
    @Value("${spring.mail.username}")
    private String fromEmail;

    @Override
    public void sendSimpleMail(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            message.setFrom(fromEmail);
            mailSender.send(message);
            log.info("이메일 전송 성공: to={}, subject={}", to, subject);
        } catch (Exception e) {
            log.error("이메일 전송 실패: to={}, subject={}, error={}", to, subject, e.getMessage());
            throw e;
        }
    }

    @Override
    public void sendScheduleReminder(String to, String scheduleTitle, String message) {
        String subject = "📅 일정 알림 - " + scheduleTitle;
        String emailContent = String.format("""
            안녕하세요! LastDance에서 보내는 일정 알림입니다.
            
            📌 일정: %s
            ⏰ 알림: %s
            
            잊지 마시고 준비해 주세요!
            
            LastDance 팀 드림
            """, scheduleTitle, message);
        
        sendSimpleMail(to, subject, emailContent);
    }

    @Override
    public void sendPaymentReminder(String to, String paymentTitle, String message) {
        String subject = "💳 납부일 알림 - " + paymentTitle;
        String emailContent = String.format("""
            안녕하세요! LastDance에서 보내는 납부일 알림입니다.
            
            💰 항목: %s
            📅 알림: %s
            
            납부를 잊지 마세요!
            
            LastDance 팀 드림
            """, paymentTitle, message);
        
        sendSimpleMail(to, subject, emailContent);
    }

    @Override
    public void sendChecklistReminder(String to, String checklistTitle, String message) {
        String subject = "✅ 할일 알림 - " + checklistTitle;
        String emailContent = String.format("""
            안녕하세요! LastDance에서 보내는 할일 알림입니다.
            
            📝 할일: %s
            ⏰ 알림: %s
            
            완료하는 것을 잊지 마세요!
            
            LastDance 팀 드림
            """, checklistTitle, message);
        
        sendSimpleMail(to, subject, emailContent);
    }
}
