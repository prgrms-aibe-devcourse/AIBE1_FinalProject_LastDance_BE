package store.lastdance.service.notification;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import store.lastdance.domain.user.OAuthProvider;

@Component
public class MailSenderRouter {

    private final JavaMailSender gmailSender;
    private final JavaMailSender naverSender;
    private final String gmailFromEmail;
    private final String naverFromEmail;

    public MailSenderRouter(
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

    public JavaMailSender getSender(OAuthProvider provider) {
        return provider.isNaverMail() ? naverSender : gmailSender;
    }

    public String getFromEmail(OAuthProvider provider) {
        return provider.isNaverMail() ? naverFromEmail : gmailFromEmail;
    }
}
