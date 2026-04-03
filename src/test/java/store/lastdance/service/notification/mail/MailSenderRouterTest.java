package store.lastdance.service.notification.mail;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;
import store.lastdance.config.MailSenderConfig;
import store.lastdance.domain.user.OAuthProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@DisplayName("MailSenderRouter 테스트")
class MailSenderRouterTest {

    private JavaMailSender gmailSender;
    private JavaMailSender naverSender;

    private MailSenderConfig gmailConfig;
    private MailSenderConfig naverConfig;

    private MailSenderRouter router;

    @BeforeEach
    void setUp() {
        gmailSender = mock(JavaMailSender.class);
        naverSender = mock(JavaMailSender.class);

        gmailConfig = new MailSenderConfig(gmailSender, "noreply@gmail.com");
        naverConfig = new MailSenderConfig(naverSender, "noreply@naver.com");

        router = new MailSenderRouter(gmailConfig, naverConfig);
    }

    // ──────────────────────────────────────────────
    // getSender
    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("getSender")
    class GetSender {

        @Test
        @DisplayName("NAVER 프로바이더이면 naverSender를 반환한다")
        void naver_returnsNaverSender() {
            assertThat(router.getSender(OAuthProvider.NAVER)).isSameAs(naverSender);
        }

        @Test
        @DisplayName("GOOGLE 프로바이더이면 gmailSender를 반환한다")
        void google_returnsGmailSender() {
            assertThat(router.getSender(OAuthProvider.GOOGLE)).isSameAs(gmailSender);
        }

        @Test
        @DisplayName("KAKAO 프로바이더이면 gmailSender를 반환한다")
        void kakao_returnsGmailSender() {
            assertThat(router.getSender(OAuthProvider.KAKAO)).isSameAs(gmailSender);
        }
    }

    // ──────────────────────────────────────────────
    // getFromEmail
    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("getFromEmail")
    class GetFromEmail {

        @Test
        @DisplayName("NAVER 프로바이더이면 naver fromEmail을 반환한다")
        void naver_returnsNaverFromEmail() {
            assertThat(router.getFromEmail(OAuthProvider.NAVER)).isEqualTo("noreply@naver.com");
        }

        @Test
        @DisplayName("GOOGLE 프로바이더이면 gmail fromEmail을 반환한다")
        void google_returnsGmailFromEmail() {
            assertThat(router.getFromEmail(OAuthProvider.GOOGLE)).isEqualTo("noreply@gmail.com");
        }

        @Test
        @DisplayName("KAKAO 프로바이더이면 gmail fromEmail을 반환한다")
        void kakao_returnsGmailFromEmail() {
            assertThat(router.getFromEmail(OAuthProvider.KAKAO)).isEqualTo("noreply@gmail.com");
        }
    }

    // ──────────────────────────────────────────────
    // getSender / getFromEmail 일관성
    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("getSender와 getFromEmail 일관성")
    class Consistency {

        @Test
        @DisplayName("NAVER는 getSender와 getFromEmail 모두 naver 설정을 사용한다")
        void naver_senderAndEmailConsistent() {
            assertThat(router.getSender(OAuthProvider.NAVER)).isSameAs(naverSender);
            assertThat(router.getFromEmail(OAuthProvider.NAVER)).isEqualTo("noreply@naver.com");
        }

        @Test
        @DisplayName("GOOGLE은 getSender와 getFromEmail 모두 gmail 설정을 사용한다")
        void google_senderAndEmailConsistent() {
            assertThat(router.getSender(OAuthProvider.GOOGLE)).isSameAs(gmailSender);
            assertThat(router.getFromEmail(OAuthProvider.GOOGLE)).isEqualTo("noreply@gmail.com");
        }

        @Test
        @DisplayName("KAKAO는 getSender와 getFromEmail 모두 gmail 설정을 사용한다")
        void kakao_senderAndEmailConsistent() {
            assertThat(router.getSender(OAuthProvider.KAKAO)).isSameAs(gmailSender);
            assertThat(router.getFromEmail(OAuthProvider.KAKAO)).isEqualTo("noreply@gmail.com");
        }
    }
}
