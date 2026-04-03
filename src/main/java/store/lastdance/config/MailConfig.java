package store.lastdance.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSenderImpl;

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

    @Bean("gmailSenderConfig")
    public MailSenderConfig gmailSenderConfig() {
        Properties props = new Properties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.ssl.enable", "false");
        props.put("mail.debug", "false");

        return buildConfig(gmailHost, gmailPort, gmailUsername, gmailPassword, props);
    }

    @Bean("naverSenderConfig")
    public MailSenderConfig naverSenderConfig() {
        Properties props = new Properties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.ssl.enable", "true");
        props.put("mail.debug", "false");

        return buildConfig(naverHost, naverPort, naverUsername, naverPassword, props);
    }

    private MailSenderConfig buildConfig(String host, int port, String username, String password, Properties props) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(host);
        sender.setPort(port);
        sender.setUsername(username);
        sender.setPassword(password);
        sender.setJavaMailProperties(props);
        return new MailSenderConfig(sender, username);
    }
}
