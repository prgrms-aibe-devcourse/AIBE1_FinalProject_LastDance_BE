package store.lastdance.service.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import store.lastdance.domain.notification.NotificationType;
import store.lastdance.exception.CustomException;
import store.lastdance.exception.ErrorCode;

@Service
@Slf4j
public class MailV2ServiceImpl implements MailV2Service {

    private final JavaMailSender gmailSender;
    private final JavaMailSender naverSender;
    private final String gmailFromEmail;
    private final String naverFromEmail;

    public MailV2ServiceImpl(
            @Qualifier("gmailSender") JavaMailSender gmailSender,
            @Qualifier("naverSender") JavaMailSender naverSender,
            @Qualifier("gmailFromEmail") String gmailFromEmail,
            @Qualifier("naverFromEmail") String naverFromEmail
    ) {
        this.gmailSender = gmailSender;
        this.naverSender = naverSender;
        this.gmailFromEmail = gmailFromEmail;
        this.naverFromEmail = naverFromEmail;
    }

    @Override
    public void sendNotification(String to, NotificationType type, String title, String content, String provider) {
        try {
            JavaMailSender sender = resolveSender(provider);
            String from = resolveFromEmail(provider);

            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setFrom(from);
            message.setSubject(resolveSubject(type, title));
            message.setText(resolveBody(type, title, content));

            sender.send(message);
        } catch (MailException e) {
            log.error("메일 발송 실패: to={}, provider={}, error={}", to, provider, e.getMessage());
            throw new CustomException(ErrorCode.NOTIFICATION_MAIL_SEND_FAILED);
        }
    }

    private JavaMailSender resolveSender(String provider) {
        return "naver".equalsIgnoreCase(provider) ? naverSender : gmailSender;
    }

    private String resolveFromEmail(String provider) {
        return "naver".equalsIgnoreCase(provider) ? naverFromEmail : gmailFromEmail;
    }

    private String resolveSubject(NotificationType type, String title) {
        return switch (type) {
            case SCHEDULE  -> "📅 일정 알림 - " + title;
            case PAYMENT   -> "💰 정산 요청 알림 - " + title;
            case CHECKLIST -> "✅ 할일 알림 - " + title;
        };
    }

    private String resolveBody(NotificationType type, String title, String content) {
        return switch (type) {
            case SCHEDULE -> String.format("""
                    안녕하세요! LastDance에서 보내는 일정 알림입니다.
                    
                    📌 일정: %s
                    ⏰ 알림: %s
                    
                    잊지 마시고 준비해 주세요!
                    
                    LastDance 팀 드림
                    """, title, content);
            case PAYMENT -> String.format("""
                    안녕하세요! LastDance에서 보내는 정산 요청 알림입니다.
                    
                    📊 지출 항목: %s
                    📅 알림: %s
                    
                    그룹 지출에 대한 정산이 요청되었습니다.
                    앱에서 확인해 주세요!
                    
                    LastDance 팀 드림
                    """, title, content);
            case CHECKLIST -> String.format("""
                    안녕하세요! LastDance에서 보내는 할일 알림입니다.
                    
                    📝 할일: %s
                    ⏰ 알림: %s
                    
                    완료하는 것을 잊지 마세요!
                    
                    LastDance 팀 드림
                    """, title, content);
        };
    }
}
