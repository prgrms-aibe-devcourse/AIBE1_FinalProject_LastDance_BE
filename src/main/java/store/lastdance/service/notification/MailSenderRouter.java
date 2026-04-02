package store.lastdance.service.notification;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import store.lastdance.config.MailSenderConfig;
import store.lastdance.domain.user.OAuthProvider;

@Component
public class MailSenderRouter {

    private final MailSenderConfig gmailConfig;
    private final MailSenderConfig naverConfig;

    public MailSenderRouter(
            @Qualifier("gmailSenderConfig") MailSenderConfig gmailConfig,
            @Qualifier("naverSenderConfig") MailSenderConfig naverConfig
    ) {
        this.gmailConfig = gmailConfig;
        this.naverConfig = naverConfig;
    }

    public JavaMailSender getSender(OAuthProvider provider) {
        return resolve(provider).sender();
    }

    public String getFromEmail(OAuthProvider provider) {
        return resolve(provider).fromEmail();
    }

    private MailSenderConfig resolve(OAuthProvider provider) {
        return provider.isNaverMail() ? naverConfig : gmailConfig;
    }
}
