package store.lastdance.service.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import store.lastdance.domain.notification.NotificationCache;
import store.lastdance.domain.notification.NotificationSetting;
import store.lastdance.domain.notification.NotificationType;
import store.lastdance.domain.user.OAuthProvider;
import store.lastdance.domain.user.User;
import store.lastdance.repository.redis.NotificationCacheRepository;
import store.lastdance.service.notification.mail.MailV2Service;
import store.lastdance.service.notification.sse.SSENotificationV2Service;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationSender 테스트")
class NotificationSenderTest {

    @Mock private NotificationCacheRepository notificationCacheRepository;
    @Mock private SSENotificationV2Service sseService;
    @Mock private MailV2Service mailService;

    @InjectMocks
    private NotificationSender notificationSender;

    private UUID userId;
    private User user;
    private NotificationSetting setting;

    private static final NotificationType TYPE = NotificationType.SCHEDULE;
    private static final String TITLE   = "일정 알림";
    private static final String CONTENT = "15분 후 시작 예정입니다.";
    private static final String RELATED_ID = "schedule-123";

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        user = mock(User.class);
        lenient().when(user.getUserId()).thenReturn(userId);
        lenient().when(user.getEmail()).thenReturn("test@example.com");
        lenient().when(user.getProvider()).thenReturn(OAuthProvider.GOOGLE);

        setting = NotificationSetting.builder().userId(userId).build();
    }

    // ──────────────────────────────────────────────
    // 캐시 중복 방지
    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("캐시 중복 방지")
    class CacheDeduplication {

        @Test
        @DisplayName("캐시가 이미 존재하면 SSE·메일·캐시 저장을 모두 건너뛴다")
        void alreadyCached_skipsAll() {
            String cacheKey = NotificationCache.generateKey(userId, TYPE, RELATED_ID);
            given(notificationCacheRepository.existsById(cacheKey)).willReturn(true);

            notificationSender.sendIfNotCached(user, setting, TYPE, TITLE, CONTENT, RELATED_ID);

            then(sseService).shouldHaveNoInteractions();
            then(mailService).shouldHaveNoInteractions();
            then(notificationCacheRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("캐시가 없으면 알림 전송 후 캐시를 저장한다")
        void notCached_sendsAndSavesCache() {
            String cacheKey = NotificationCache.generateKey(userId, TYPE, RELATED_ID);
            given(notificationCacheRepository.existsById(cacheKey)).willReturn(false);
            setting.updateSSEEnabled(true);
            given(sseService.sendNotification(any(), any(), any(), any(), any())).willReturn(true);

            notificationSender.sendIfNotCached(user, setting, TYPE, TITLE, CONTENT, RELATED_ID);

            then(notificationCacheRepository).should().save(any(NotificationCache.class));
        }
    }

    // ──────────────────────────────────────────────
    // SSE 전송
    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("SSE 전송")
    class SseSend {

        @BeforeEach
        void noCacheExists() {
            given(notificationCacheRepository.existsById(anyString())).willReturn(false);
        }

        @Test
        @DisplayName("SSE가 활성화되어 있으면 sendNotification을 호출한다")
        void sseEnabled_callsSendNotification() {
            setting.updateSSEEnabled(true);
            given(sseService.sendNotification(userId, TITLE, CONTENT, TYPE, RELATED_ID)).willReturn(true);

            notificationSender.sendIfNotCached(user, setting, TYPE, TITLE, CONTENT, RELATED_ID);

            then(sseService).should().sendNotification(userId, TITLE, CONTENT, TYPE, RELATED_ID);
        }

        @Test
        @DisplayName("SSE가 비활성화되어 있으면 sendNotification을 호출하지 않는다")
        void sseDisabled_skipsSSE() {
            setting.updateSSEEnabled(false);

            notificationSender.sendIfNotCached(user, setting, TYPE, TITLE, CONTENT, RELATED_ID);

            then(sseService).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("SSE 전송 중 예외가 발생해도 메일 전송과 캐시 저장은 계속된다")
        void sseThrows_continuesMailAndCache() {
            setting.updateSSEEnabled(true);
            setting.updateEmailEnabled(true);
            setting.updateScheduleReminder(true);
            given(sseService.sendNotification(any(), any(), any(), any(), any()))
                    .willThrow(new RuntimeException("SSE 연결 오류"));

            notificationSender.sendIfNotCached(user, setting, TYPE, TITLE, CONTENT, RELATED_ID);

            then(mailService).should().sendNotification(
                    user.getEmail(), TYPE, TITLE, CONTENT, user.getProvider());
            then(notificationCacheRepository).should().save(any());
        }
    }

    // ──────────────────────────────────────────────
    // 메일 전송
    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("메일 전송")
    class MailSend {

        @BeforeEach
        void noCacheExists() {
            given(notificationCacheRepository.existsById(anyString())).willReturn(false);
        }

        @Test
        @DisplayName("이메일과 해당 타입 알림이 모두 활성화되어 있으면 메일을 전송한다")
        void emailEnabledForType_sendsMail() {
            setting.updateEmailEnabled(true);
            setting.updateScheduleReminder(true);

            notificationSender.sendIfNotCached(user, setting, TYPE, TITLE, CONTENT, RELATED_ID);

            then(mailService).should().sendNotification(
                    user.getEmail(), TYPE, TITLE, CONTENT, user.getProvider());
        }

        @Test
        @DisplayName("이메일이 비활성화되어 있으면 메일을 전송하지 않는다")
        void emailDisabled_skipsMail() {
            setting.updateEmailEnabled(false);
            setting.updateScheduleReminder(true);

            notificationSender.sendIfNotCached(user, setting, TYPE, TITLE, CONTENT, RELATED_ID);

            then(mailService).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("이메일이 활성화되어 있어도 해당 타입 알림이 꺼져 있으면 메일을 전송하지 않는다")
        void emailEnabledButTypeDisabled_skipsMail() {
            setting.updateEmailEnabled(true);
            setting.updateScheduleReminder(false);

            notificationSender.sendIfNotCached(user, setting, TYPE, TITLE, CONTENT, RELATED_ID);

            then(mailService).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("메일 전송 중 예외가 발생해도 캐시 저장은 계속된다")
        void mailThrows_continuesCache() {
            setting.updateEmailEnabled(true);
            setting.updateScheduleReminder(true);
            doThrow(new RuntimeException("SMTP 오류"))
                    .when(mailService).sendNotification(any(), any(), any(), any(), any());

            notificationSender.sendIfNotCached(user, setting, TYPE, TITLE, CONTENT, RELATED_ID);

            then(notificationCacheRepository).should().save(any());
        }
    }

    // ──────────────────────────────────────────────
    // 캐시 저장
    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("캐시 저장")
    class CacheSave {

        @BeforeEach
        void noCacheExists() {
            given(notificationCacheRepository.existsById(anyString())).willReturn(false);
        }

        @Test
        @DisplayName("알림 전송 후 올바른 키로 캐시를 저장한다")
        void savesCache_withCorrectKey() {
            notificationSender.sendIfNotCached(user, setting, TYPE, TITLE, CONTENT, RELATED_ID);

            then(notificationCacheRepository).should().save(argThat(cache ->
                    cache.getId().equals(NotificationCache.generateKey(userId, TYPE, RELATED_ID))
            ));
        }

        @Test
        @DisplayName("캐시 저장 중 예외가 발생해도 메서드가 정상 종료된다")
        void cacheThrows_doesNotPropagate() {
            doThrow(new RuntimeException("Redis 연결 오류"))
                    .when(notificationCacheRepository).save(any());

            org.assertj.core.api.Assertions.assertThatCode(() ->
                    notificationSender.sendIfNotCached(user, setting, TYPE, TITLE, CONTENT, RELATED_ID)
            ).doesNotThrowAnyException();
        }
    }

    // ──────────────────────────────────────────────
    // 전체 흐름
    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("전체 흐름")
    class FullFlow {

        @Test
        @DisplayName("SSE·메일 모두 활성화 시 SSE → 메일 → 캐시 저장 순서로 실행된다")
        void allEnabled_sseMailCache_inOrder() {
            given(notificationCacheRepository.existsById(anyString())).willReturn(false);
            setting.updateSSEEnabled(true);
            setting.updateEmailEnabled(true);
            setting.updateScheduleReminder(true);
            given(sseService.sendNotification(any(), any(), any(), any(), any())).willReturn(true);

            notificationSender.sendIfNotCached(user, setting, TYPE, TITLE, CONTENT, RELATED_ID);

            var inOrder = inOrder(sseService, mailService, notificationCacheRepository);
            inOrder.verify(sseService).sendNotification(any(), any(), any(), any(), any());
            inOrder.verify(mailService).sendNotification(any(), any(), any(), any(), any());
            inOrder.verify(notificationCacheRepository).save(any());
        }

        @Test
        @DisplayName("SSE·메일 모두 비활성화 시에도 캐시는 저장된다")
        void allDisabled_stillSavesCache() {
            given(notificationCacheRepository.existsById(anyString())).willReturn(false);
            setting.updateSSEEnabled(false);
            setting.updateEmailEnabled(false);

            notificationSender.sendIfNotCached(user, setting, TYPE, TITLE, CONTENT, RELATED_ID);

            then(sseService).shouldHaveNoInteractions();
            then(mailService).shouldHaveNoInteractions();
            then(notificationCacheRepository).should().save(any());
        }
    }
}
