package store.lastdance.service.notification.mail;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import store.lastdance.domain.notification.NotificationType;
import store.lastdance.domain.user.OAuthProvider;
import store.lastdance.exception.CustomException;
import store.lastdance.exception.ErrorCode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
@DisplayName("MailV2ServiceImpl 테스트")
class MailV2ServiceImplTest {

    @Mock
    private MailSenderRouter mailSenderRouter;

    @Mock
    private JavaMailSender gmailSender;

    @Mock
    private JavaMailSender naverSender;

    @InjectMocks
    private MailV2ServiceImpl mailService;

    private static final String TO      = "user@example.com";
    private static final String TITLE   = "팀 미팅";
    private static final String CONTENT = "15분 후 시작 예정입니다.";

    // ──────────────────────────────────────────────
    // 라우팅
    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("발신자 라우팅")
    class SenderRouting {

        @Test
        @DisplayName("GOOGLE 프로바이더이면 gmailSender로 메일을 전송한다")
        void google_usesGmailSender() {
            given(mailSenderRouter.getSender(OAuthProvider.GOOGLE)).willReturn(gmailSender);
            given(mailSenderRouter.getFromEmail(OAuthProvider.GOOGLE)).willReturn("noreply@gmail.com");

            mailService.sendNotification(TO, NotificationType.SCHEDULE, TITLE, CONTENT, OAuthProvider.GOOGLE);

            then(gmailSender).should().send(any(SimpleMailMessage.class));
            then(naverSender).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("KAKAO 프로바이더이면 gmailSender로 메일을 전송한다")
        void kakao_usesGmailSender() {
            given(mailSenderRouter.getSender(OAuthProvider.KAKAO)).willReturn(gmailSender);
            given(mailSenderRouter.getFromEmail(OAuthProvider.KAKAO)).willReturn("noreply@gmail.com");

            mailService.sendNotification(TO, NotificationType.PAYMENT, TITLE, CONTENT, OAuthProvider.KAKAO);

            then(gmailSender).should().send(any(SimpleMailMessage.class));
            then(naverSender).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("NAVER 프로바이더이면 naverSender로 메일을 전송한다")
        void naver_usesNaverSender() {
            given(mailSenderRouter.getSender(OAuthProvider.NAVER)).willReturn(naverSender);
            given(mailSenderRouter.getFromEmail(OAuthProvider.NAVER)).willReturn("noreply@naver.com");

            mailService.sendNotification(TO, NotificationType.CHECKLIST, TITLE, CONTENT, OAuthProvider.NAVER);

            then(naverSender).should().send(any(SimpleMailMessage.class));
            then(gmailSender).shouldHaveNoInteractions();
        }
    }

    // ──────────────────────────────────────────────
    // 메시지 필드 검증
    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("메시지 필드 검증")
    class MessageFields {

        @BeforeEach
        void usesGmail() {
            given(mailSenderRouter.getSender(OAuthProvider.GOOGLE)).willReturn(gmailSender);
            given(mailSenderRouter.getFromEmail(OAuthProvider.GOOGLE)).willReturn("noreply@gmail.com");
        }

        @Test
        @DisplayName("수신자(To)가 전달받은 이메일로 설정된다")
        void to_isSetCorrectly() {
            mailService.sendNotification(TO, NotificationType.SCHEDULE, TITLE, CONTENT, OAuthProvider.GOOGLE);

            SimpleMailMessage sent = captureMessage();
            assertThat(sent.getTo()).containsExactly(TO);
        }

        @Test
        @DisplayName("발신자(From)가 라우터에서 반환한 fromEmail로 설정된다")
        void from_isSetFromRouter() {
            mailService.sendNotification(TO, NotificationType.SCHEDULE, TITLE, CONTENT, OAuthProvider.GOOGLE);

            SimpleMailMessage sent = captureMessage();
            assertThat(sent.getFrom()).isEqualTo("noreply@gmail.com");
        }

        @Test
        @DisplayName("제목(Subject)이 MailTemplate.buildSubject 결과와 동일하다")
        void subject_matchesMailTemplate() {
            mailService.sendNotification(TO, NotificationType.SCHEDULE, TITLE, CONTENT, OAuthProvider.GOOGLE);

            String expected = MailTemplate.buildSubject(NotificationType.SCHEDULE, TITLE);
            SimpleMailMessage sent = captureMessage();
            assertThat(sent.getSubject()).isEqualTo(expected);
        }

        @Test
        @DisplayName("본문(Text)이 MailTemplate.buildBody 결과와 동일하다")
        void body_matchesMailTemplate() {
            mailService.sendNotification(TO, NotificationType.SCHEDULE, TITLE, CONTENT, OAuthProvider.GOOGLE);

            String expected = MailTemplate.buildBody(NotificationType.SCHEDULE, TITLE, CONTENT);
            SimpleMailMessage sent = captureMessage();
            assertThat(sent.getText()).isEqualTo(expected);
        }

        @Test
        @DisplayName("PAYMENT 타입의 제목과 본문이 각각 PAYMENT 기준으로 생성된다")
        void paymentType_subjectAndBodyUsePayment() {
            mailService.sendNotification(TO, NotificationType.PAYMENT, TITLE, CONTENT, OAuthProvider.GOOGLE);

            SimpleMailMessage sent = captureMessage();
            assertThat(sent.getSubject()).isEqualTo(MailTemplate.buildSubject(NotificationType.PAYMENT, TITLE));
            assertThat(sent.getText()).isEqualTo(MailTemplate.buildBody(NotificationType.PAYMENT, TITLE, CONTENT));
        }

        @Test
        @DisplayName("CHECKLIST 타입의 제목과 본문이 각각 CHECKLIST 기준으로 생성된다")
        void checklistType_subjectAndBodyUseChecklist() {
            mailService.sendNotification(TO, NotificationType.CHECKLIST, TITLE, CONTENT, OAuthProvider.GOOGLE);

            SimpleMailMessage sent = captureMessage();
            assertThat(sent.getSubject()).isEqualTo(MailTemplate.buildSubject(NotificationType.CHECKLIST, TITLE));
            assertThat(sent.getText()).isEqualTo(MailTemplate.buildBody(NotificationType.CHECKLIST, TITLE, CONTENT));
        }

        private SimpleMailMessage captureMessage() {
            ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
            then(gmailSender).should().send(captor.capture());
            return captor.getValue();
        }
    }

    // ──────────────────────────────────────────────
    // 예외 처리
    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("예외 처리")
    class ExceptionHandling {

        @BeforeEach
        void usesGmail() {
            given(mailSenderRouter.getSender(OAuthProvider.GOOGLE)).willReturn(gmailSender);
            given(mailSenderRouter.getFromEmail(OAuthProvider.GOOGLE)).willReturn("noreply@gmail.com");
        }

        @Test
        @DisplayName("MailException 발생 시 NOTIFICATION_MAIL_SEND_FAILED 예외로 변환한다")
        void mailException_wrappedAsCustomException() {
            doThrow(new MailSendException("SMTP 오류"))
                    .when(gmailSender).send(any(SimpleMailMessage.class));

            assertThatThrownBy(() ->
                    mailService.sendNotification(TO, NotificationType.SCHEDULE, TITLE, CONTENT, OAuthProvider.GOOGLE))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.NOTIFICATION_MAIL_SEND_FAILED));
        }

        @Test
        @DisplayName("MailException이 아닌 RuntimeException은 그대로 전파된다")
        void nonMailException_propagatesAsIs() {
            doThrow(new RuntimeException("예상치 못한 오류"))
                    .when(gmailSender).send(any(SimpleMailMessage.class));

            assertThatThrownBy(() ->
                    mailService.sendNotification(TO, NotificationType.SCHEDULE, TITLE, CONTENT, OAuthProvider.GOOGLE))
                    .isInstanceOf(RuntimeException.class)
                    .isNotInstanceOf(CustomException.class);
        }
    }
}
