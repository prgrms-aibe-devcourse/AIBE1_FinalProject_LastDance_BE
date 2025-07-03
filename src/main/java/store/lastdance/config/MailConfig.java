package store.lastdance.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.util.StringUtils;

import java.util.Properties;

@Configuration
@Slf4j
public class MailConfig {

    // Gmail 설정
    @Value("${spring.mail.gmail.host:smtp.gmail.com}")
    private String gmailHost;
    @Value("${spring.mail.gmail.port:587}")
    private int gmailPort;
    @Value("${spring.mail.gmail.username:}")
    private String gmailUsername;
    @Value("${spring.mail.gmail.password:}")
    private String gmailPassword;

    // 네이버 설정
    @Value("${spring.mail.naver.host:smtp.naver.com}")
    private String naverHost;
    @Value("${spring.mail.naver.port:465}")
    private int naverPort;
    @Value("${spring.mail.naver.username:}")
    private String naverUsername;
    @Value("${spring.mail.naver.password:}")
    private String naverPassword;

    @Bean("gmailSender")
    public JavaMailSender gmailSender() {
        if (!StringUtils.hasText(gmailUsername) || !StringUtils.hasText(gmailPassword)) {
            log.warn("Gmail 설정이 없습니다. Gmail 메일 발송이 비활성화됩니다.");
            return null;
        }

        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(gmailHost);
        mailSender.setPort(gmailPort);
        mailSender.setUsername(gmailUsername);
        mailSender.setPassword(gmailPassword);

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");  // Gmail은 STARTTLS 사용
        props.put("mail.smtp.ssl.trust", gmailHost);
        props.put("mail.debug", "false");

        log.info("Gmail 메일 설정 완료 - 호스트: {}, 포트: {}, 사용자: {}", 
                gmailHost, gmailPort, gmailUsername);
        
        return mailSender;
    }

    @Bean("naverSender")
    public JavaMailSender naverSender() {
        if (!StringUtils.hasText(naverUsername) || !StringUtils.hasText(naverPassword)) {
            log.warn("네이버 설정이 없습니다. 네이버 메일 발송이 비활성화됩니다.");
            return null;
        }

        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(naverHost);
        mailSender.setPort(naverPort);
        mailSender.setUsername(naverUsername);
        mailSender.setPassword(naverPassword);

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.ssl.enable", "true");  // 네이버는 SSL 사용
        props.put("mail.smtp.ssl.trust", naverHost);
        props.put("mail.debug", "false");

        log.info("네이버 메일 설정 완료 - 호스트: {}, 포트: {}, 사용자: {}", 
                naverHost, naverPort, naverUsername);
        
        return mailSender;
    }

    @Bean
    @Primary
    public JavaMailSender defaultMailSender() {
        // Gmail 설정을 우선으로 사용
        JavaMailSender gmailSender = gmailSender();
        if (gmailSender != null) {
            log.info("기본 메일 서비스로 Gmail 사용");
            return gmailSender;
        }
        
        // Gmail이 없으면 네이버 사용
        JavaMailSender naverSender = naverSender();
        if (naverSender != null) {
            log.info("기본 메일 서비스로 네이버 사용");
            return naverSender;
        }
        
        throw new IllegalStateException("메일 설정이 없습니다. Gmail 또는 네이버 메일 설정을 확인하세요.");
    }
}
