package store.lastdance.config;

import org.springframework.mail.javamail.JavaMailSender;

public record MailSenderConfig(JavaMailSender sender, String fromEmail) {}
