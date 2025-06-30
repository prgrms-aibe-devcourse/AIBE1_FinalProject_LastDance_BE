package store.lastdance.service.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailServiceImpl implements MailService {

    private final JavaMailSender defaultMailSender;
    
    @Autowired(required = false)
    @Qualifier("gmailSender")
    private JavaMailSender gmailSender;
    
    @Autowired(required = false)
    @Qualifier("naverSender")
    private JavaMailSender naverSender;
    
    @Value("${spring.mail.gmail.username:${GOOGLE_EMAIL:}}")
    private String gmailFrom;
    
    @Value("${spring.mail.naver.username:${NAVER_EMAIL:}}")
    private String naverFrom;

    @Override
    public void sendSimpleMail(String to, String subject, String text) {
        sendSimpleMail(to, subject, text, "gmail"); // 기본값으로 gmail 사용
    }

    @Override
    public void sendSimpleMail(String to, String subject, String text, String provider) {
        try {
            JavaMailSender mailSender = getMailSender(provider);
            String fromEmail = getFromEmail(provider);
            
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            message.setFrom(fromEmail);
            
            mailSender.send(message);
            log.info("이메일 전송 성공 [{}]: to={}, subject={}", provider.toUpperCase(), to, subject);
            
        } catch (Exception e) {
            log.error("이메일 전송 실패 [{}]: to={}, subject={}, error={}", 
                provider.toUpperCase(), to, subject, e.getMessage());
            
            // 실패시 다른 서비스로 재시도
            String fallbackProvider = "gmail".equals(provider) ? "naver" : "gmail";
            if (isProviderAvailable(fallbackProvider)) {
                log.info("대체 메일 서비스로 재시도: {}", fallbackProvider);
                sendSimpleMail(to, subject, text, fallbackProvider);
            } else {
                throw new RuntimeException("모든 메일 서비스 발송 실패: " + e.getMessage());
            }
        }
    }

    @Override
    public void sendScheduleReminder(String to, String scheduleTitle, String message) {
        sendScheduleReminder(to, scheduleTitle, message, "gmail"); // 기본값
    }

    @Override
    public void sendScheduleReminder(String to, String scheduleTitle, String message, String provider) {
        String subject = "📅 일정 알림 - " + scheduleTitle;
        String emailContent = String.format("""
            안녕하세요! LastDance에서 보내는 일정 알림입니다.
            
            📌 일정: %s
            ⏰ 알림: %s
            
            잊지 마시고 준비해 주세요!
            
            LastDance 팀 드림
            """, scheduleTitle, message);
        
        sendSimpleMail(to, subject, emailContent, provider);
    }

    @Override
    public void sendPaymentReminder(String to, String paymentTitle, String message) {
        sendPaymentReminder(to, paymentTitle, message, "gmail"); // 기본값
    }

    @Override
    public void sendPaymentReminder(String to, String paymentTitle, String message, String provider) {
        String subject = "💳 납부일 알림 - " + paymentTitle;
        String emailContent = String.format("""
            안녕하세요! LastDance에서 보내는 납부일 알림입니다.
            
            💰 항목: %s
            📅 알림: %s
            
            납부를 잊지 마세요!
            
            LastDance 팀 드림
            """, paymentTitle, message);
        
        sendSimpleMail(to, subject, emailContent, provider);
    }

    @Override
    public void sendChecklistReminder(String to, String checklistTitle, String message) {
        sendChecklistReminder(to, checklistTitle, message, "gmail"); // 기본값
    }

    @Override
    public void sendChecklistReminder(String to, String checklistTitle, String message, String provider) {
        String subject = "✅ 할일 알림 - " + checklistTitle;
        String emailContent = String.format("""
            안녕하세요! LastDance에서 보내는 할일 알림입니다.
            
            📝 할일: %s
            ⏰ 알림: %s
            
            완료하는 것을 잊지 마세요!
            
            LastDance 팀 드림
            """, checklistTitle, message);
        
        sendSimpleMail(to, subject, emailContent, provider);
    }

    @Override
    public boolean isProviderAvailable(String provider) {
        try {
            JavaMailSender sender = getMailSender(provider);
            return sender != null;
        } catch (Exception e) {
            log.warn("메일 서비스 사용 불가: {}", provider);
            return false;
        }
    }

    private JavaMailSender getMailSender(String provider) {
        return switch (provider.toLowerCase()) {
            case "gmail" -> gmailSender != null ? gmailSender : defaultMailSender;
            case "naver" -> naverSender != null ? naverSender : defaultMailSender;
            default -> defaultMailSender;
        };
    }

    private String getFromEmail(String provider) {
        return switch (provider.toLowerCase()) {
            case "gmail" -> gmailFrom != null && !gmailFrom.isEmpty() ? gmailFrom : "noreply@lastdance.com";
            case "naver" -> naverFrom != null && !naverFrom.isEmpty() ? naverFrom : "noreply@lastdance.com";
            default -> gmailFrom != null && !gmailFrom.isEmpty() ? gmailFrom : "noreply@lastdance.com";
        };
    }
}
