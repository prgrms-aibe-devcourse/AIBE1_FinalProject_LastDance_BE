package store.lastdance.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.ssl.enable", "false");
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
        props.put("mail.smtp.ssl.enable", "true");
        props.put("mail.debug", "false");

        return mailSender;
    }

    @Bean("gmailFromEmail")
    public String gmailFromEmail() {
        return StringUtils.hasText(gmailUsername) ? gmailUsername : "lastdance857@gmail.com";
    }

    @Bean("naverFromEmail")
    public String naverFromEmail() {
        return StringUtils.hasText(naverUsername) ? naverUsername : "lastdance857@naver.com";
    }
}
