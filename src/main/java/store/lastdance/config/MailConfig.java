package store.lastdance.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.util.StringUtils;

import java.util.Properties;

@Configuration
public class MailConfig {

    // Gmail 설정
    @Value("${spring.mail.gmail.host}")
    private String gmailHost;
    @Value("${spring.mail.gmail.port}")
    private int gmailPort;
    @Value("${spring.mail.gmail.username}")
    private String gmailUsername;
    @Value("${spring.mail.gmail.password}")
    private String gmailPassword;

    // 네이버 설정
    @Value("${spring.mail.naver.host}")
    private String naverHost;
    @Value("${spring.mail.naver.port}")
    private int naverPort;
    @Value("${spring.mail.naver.username}")
    private String naverUsername;
    @Value("${spring.mail.naver.password}")
    private String naverPassword;

    @Bean("gmailSender")
    public JavaMailSender gmailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(gmailHost);
        mailSender.setPort(gmailPort);
        mailSender.setUsername(gmailUsername);
        mailSender.setPassword(gmailPassword);

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.ssl.enable", "true");  // SSL 사용
        props.put("mail.debug", "false");

        return mailSender;
    }

    @Bean("naverSender")
    public JavaMailSender naverSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(naverHost);
        mailSender.setPort(naverPort);
        mailSender.setUsername(naverUsername);
        mailSender.setPassword(naverPassword);

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.ssl.enable", "true");  // SSL 사용
        props.put("mail.debug", "false");

        return mailSender;
    }

    @Bean
    @Primary
    public JavaMailSender defaultMailSender() {
        // Gmail 설정이 있으면 Gmail 사용, 없으면 네이버 사용
        if (StringUtils.hasText(gmailUsername)) {
            return gmailSender();
        } else if (StringUtils.hasText(naverUsername)) {
            return naverSender();
        } else {
            throw new IllegalStateException("메일 설정이 없습니다. GOOGLE_EMAIL 또는 NAVER_EMAIL 환경변수를 설정하세요.");
        }
    }
}
