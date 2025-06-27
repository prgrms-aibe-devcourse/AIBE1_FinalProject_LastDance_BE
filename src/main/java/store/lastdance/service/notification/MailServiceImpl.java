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

    public void sendSimpleMail(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            message.setFrom(fromEmail); // 설정에서 가져온 이메일 주소 사용
            mailSender.send(message);
            log.info("이메일 전송 성공: to={}, subject={}", to, subject);
        } catch (Exception e) {
            log.error("이메일 전송 실패: to={}, subject={}, error={}", to, subject, e.getMessage());
            throw e; // 상위에서 처리할 수 있도록 예외 재발생
        }
    }
}
