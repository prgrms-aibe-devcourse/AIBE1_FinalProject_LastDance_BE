package store.lastdance.service.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import store.lastdance.domain.notification.NotificationSetting;
import store.lastdance.domain.notification.NotificationType;
import store.lastdance.domain.user.User;
import store.lastdance.repository.notification.NotificationSettingRepository;
import store.lastdance.service.notification.checker.NotificationChecker;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationProcessServiceImpl 테스트")
class NotificationProcessServiceImplTest {

    @Mock
    private NotificationSettingRepository settingRepository;
    @Mock
    private NotificationChecker scheduleChecker;
    @Mock
    private NotificationChecker paymentChecker;
    @Mock
    private NotificationChecker checklistChecker;

    private NotificationProcessServiceImpl service;

    // @InjectMocks 대신 직접 생성 — checkers 리스트를 생성자에 넘겨야 하기 때문
    @BeforeEach
    void setUp() {
        given(scheduleChecker.getType()).willReturn(NotificationType.SCHEDULE);
        given(paymentChecker.getType()).willReturn(NotificationType.PAYMENT);
        given(checklistChecker.getType()).willReturn(NotificationType.CHECKLIST);

        service = new NotificationProcessServiceImpl(
                settingRepository,
                List.of(scheduleChecker, paymentChecker, checklistChecker)
        );
    }

    // ──────────────────────────────────────────────
    // findAllActiveSettings
    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("findAllActiveSettings")
    class FindAllActiveSettings {

        @Test
        @DisplayName("repository에서 반환한 리스트를 그대로 반환한다")
        void returnsRepositoryResult() {
            UUID userId = UUID.randomUUID();
            NotificationSetting setting = buildSetting(userId, true, true);
            given(settingRepository.findAllActiveWithUser()).willReturn(List.of(setting));

            List<NotificationSetting> result = service.findAllActiveSettings();

            assertThat(result).hasSize(1);
            then(settingRepository).should().findAllActiveWithUser();
        }

        @Test
        @DisplayName("활성 설정이 없으면 빈 리스트를 반환한다")
        void emptyList_whenNoActiveSettings() {
            given(settingRepository.findAllActiveWithUser()).willReturn(List.of());

            List<NotificationSetting> result = service.findAllActiveSettings();

            assertThat(result).isEmpty();
        }
    }

    // ──────────────────────────────────────────────
    // checkAndSendNotifications
    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("checkAndSendNotifications")
    class CheckAndSendNotifications {

        @Test
        @DisplayName("세 알림 타입이 모두 활성화되면 세 checker가 모두 실행된다")
        void allEnabled_runsAllThreeCheckers() {
            UUID userId = UUID.randomUUID();
            User user = mockUser(userId);
            NotificationSetting setting = buildSetting(userId, true, true);

            service.checkAndSendNotifications(user, setting);

            then(scheduleChecker).should().check(eq(user), eq(setting), any(LocalDateTime.class));
            then(paymentChecker).should().check(eq(user), eq(setting), any(LocalDateTime.class));
            then(checklistChecker).should().check(eq(user), eq(setting), any(LocalDateTime.class));
        }

        @Test
        @DisplayName("일정 알림이 비활성화되면 scheduleChecker는 실행되지 않는다")
        void scheduleDisabled_skipsScheduleChecker() {
            UUID userId = UUID.randomUUID();
            User user = mockUser(userId);
            NotificationSetting setting = buildSetting(userId, true, true);
            setting.updateScheduleReminder(false);

            service.checkAndSendNotifications(user, setting);

            then(scheduleChecker).should(never()).check(any(), any(), any());
            then(paymentChecker).should().check(eq(user), eq(setting), any(LocalDateTime.class));
            then(checklistChecker).should().check(eq(user), eq(setting), any(LocalDateTime.class));
        }

        @Test
        @DisplayName("정산 알림이 비활성화되면 paymentChecker는 실행되지 않는다")
        void paymentDisabled_skipsPaymentChecker() {
            UUID userId = UUID.randomUUID();
            User user = mockUser(userId);
            NotificationSetting setting = buildSetting(userId, true, true);
            setting.updatePaymentReminder(false);

            service.checkAndSendNotifications(user, setting);

            then(scheduleChecker).should().check(eq(user), eq(setting), any(LocalDateTime.class));
            then(paymentChecker).should(never()).check(any(), any(), any());
            then(checklistChecker).should().check(eq(user), eq(setting), any(LocalDateTime.class));
        }

        @Test
        @DisplayName("체크리스트 알림이 비활성화되면 checklistChecker는 실행되지 않는다")
        void checklistDisabled_skipsChecklistChecker() {
            UUID userId = UUID.randomUUID();
            User user = mockUser(userId);
            NotificationSetting setting = buildSetting(userId, true, true);
            setting.updateChecklistReminder(false);

            service.checkAndSendNotifications(user, setting);

            then(scheduleChecker).should().check(eq(user), eq(setting), any(LocalDateTime.class));
            then(paymentChecker).should().check(eq(user), eq(setting), any(LocalDateTime.class));
            then(checklistChecker).should(never()).check(any(), any(), any());
        }

        @Test
        @DisplayName("특정 checker에서 예외가 발생해도 나머지 checker는 계속 실행된다")
        void checkerThrowsException_othersStillRun() {
            UUID userId = UUID.randomUUID();
            User user = mockUser(userId);
            NotificationSetting setting = buildSetting(userId, true, true);

            doThrow(new RuntimeException("checker 오류")).when(scheduleChecker)
                    .check(any(), any(), any());

            service.checkAndSendNotifications(user, setting);

            then(paymentChecker).should().check(eq(user), eq(setting), any(LocalDateTime.class));
            then(checklistChecker).should().check(eq(user), eq(setting), any(LocalDateTime.class));
        }

        @Test
        @DisplayName("모든 알림이 비활성화되면 어떤 checker도 실행되지 않는다")
        void allDisabled_noCheckerRuns() {
            UUID userId = UUID.randomUUID();
            User user = mockUser(userId);
            NotificationSetting setting = NotificationSetting.builder().userId(userId).build();

            service.checkAndSendNotifications(user, setting);

            then(scheduleChecker).should(never()).check(any(), any(), any());
            then(paymentChecker).should(never()).check(any(), any(), any());
            then(checklistChecker).should(never()).check(any(), any(), any());
        }
    }

    // ──────────────────────────────────────────────
    // helpers
    // ──────────────────────────────────────────────

    private User mockUser(UUID userId) {
        User user = mock(User.class);
        lenient().when(user.getUserId()).thenReturn(userId);
        return user;
    }

    private NotificationSetting buildSetting(UUID userId, boolean sseEnabled, boolean emailEnabled) {
        NotificationSetting setting = NotificationSetting.builder().userId(userId).build();
        setting.updateSSEEnabled(sseEnabled);
        setting.updateEmailEnabled(emailEnabled);
        setting.updateScheduleReminder(true);
        setting.updatePaymentReminder(true);
        setting.updateChecklistReminder(true);
        return setting;
    }
}