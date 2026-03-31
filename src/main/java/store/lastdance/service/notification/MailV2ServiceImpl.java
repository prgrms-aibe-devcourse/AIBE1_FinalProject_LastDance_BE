package store.lastdance.service.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import store.lastdance.exception.CustomException;
import store.lastdance.exception.ErrorCode;

@Service
@Slf4j
public class MailV2ServiceImpl implements MailV2Service {

    private final JavaMailSender defaultMailSender;
    private final String gmailFromEmail;
    private final String naverFromEmail;

    public MailV2ServiceImpl(
            JavaMailSender defaultMailSender,
            @Qualifier("gmailFromEmail") String gmailFromEmail,
            @Qualifier("naverFromEmail") String naverFromEmail
    ) {
        this.defaultMailSender = defaultMailSender;
        this.gmailFromEmail = gmailFromEmail;
        this.naverFromEmail = naverFromEmail;
    }

    @Override
    public void sendSimpleMail(String to, String subject, String text, String provider) {
        try {
            String fromEmail = getFromEmail(provider);

            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            message.setFrom(fromEmail);

            defaultMailSender.send(message);
        } catch (MailException e) {
            log.error("메일 발송 실패: to={}, provider={}, error={}", to, provider, e.getMessage());
            throw new CustomException(ErrorCode.NOTIFICATION_MAIL_SEND_FAILED);
        }
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
    public void sendPaymentReminder(String to, String paymentTitle, String message, String provider) {
        String subject = "💰 정산 요청 알림 - " + paymentTitle;
        String emailContent = String.format("""
            안녕하세요! LastDance에서 보내는 정산 요청 알림입니다.
            
            📊 지출 항목: %s
            📅 알림: %s
            
            그룹 지출에 대한 정산이 요청되었습니다.
            앱에서 확인해 주세요!
            
            LastDance 팀 드림
            """, paymentTitle, message);

        sendSimpleMail(to, subject, emailContent, provider);
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

    private String getFromEmail(String provider) {
        return switch (provider.toLowerCase()) {
            case "naver" -> naverFromEmail;
            default -> gmailFromEmail;
        };
    }
}
