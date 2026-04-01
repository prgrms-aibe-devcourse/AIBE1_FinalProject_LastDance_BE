package store.lastdance.service.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import store.lastdance.domain.notification.NotificationType;
import store.lastdance.domain.user.OAuthProvider;
import store.lastdance.exception.CustomException;
import store.lastdance.exception.ErrorCode;

@Service
@Slf4j
@RequiredArgsConstructor
public class MailV2ServiceImpl implements MailV2Service {

    private final MailSenderRouter mailSenderRouter;

    @Override
    public void sendNotification(String to, NotificationType type, String title, String content, OAuthProvider provider) {
        try {
            JavaMailSender sender = mailSenderRouter.getSender(provider);
            String from = mailSenderRouter.getFromEmail(provider);

            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setFrom(from);
            message.setSubject(type.buildSubject(title));
            message.setText(type.buildBody(title, content));

            sender.send(message);
        } catch (MailException e) {
            log.error("메일 발송 실패: to={}, provider={}, error={}", to, provider, e.getMessage());
            throw new CustomException(ErrorCode.NOTIFICATION_MAIL_SEND_FAILED);
        }
    }
}
