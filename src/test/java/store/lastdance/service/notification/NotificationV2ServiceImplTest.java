package store.lastdance.service.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import store.lastdance.domain.notification.NotificationType;
import store.lastdance.exception.CustomException;
import store.lastdance.exception.ErrorCode;
import store.lastdance.repository.redis.NotificationReadRepository;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationV2ServiceImpl 테스트")
class NotificationV2ServiceImplTest {

    @Mock
    private NotificationReadRepository notificationReadRepository;

    @InjectMocks
    private NotificationV2ServiceImpl service;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
    }

    // ──────────────────────────────────────────────
    // markNotificationAsRead
    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("markNotificationAsRead")
    class MarkNotificationAsRead {

        @Test
        @DisplayName("유효한 notificationId이면 NotificationRead를 저장한다")
        void validId_savesNotificationRead() {
            String notificationId = userId + ":SCHEDULE:schedule-42";

            service.markNotificationAsRead(userId, notificationId);

            then(notificationReadRepository).should().save(argThat(read ->
                    read.getId().equals(notificationId)
                    && read.getUserId().equals(userId)
                    && read.getType() == NotificationType.SCHEDULE
                    && read.getRelatedId().equals("schedule-42")
            ));
        }

        @Test
        @DisplayName("PAYMENT 타입도 올바르게 파싱하여 저장한다")
        void paymentType_parsedAndSaved() {
            String notificationId = userId + ":PAYMENT:payment-99";

            service.markNotificationAsRead(userId, notificationId);

            then(notificationReadRepository).should().save(argThat(read ->
                    read.getType() == NotificationType.PAYMENT
                    && read.getRelatedId().equals("payment-99")
            ));
        }

        @Test
        @DisplayName("CHECKLIST 타입도 올바르게 파싱하여 저장한다")
        void checklistType_parsedAndSaved() {
            String notificationId = userId + ":CHECKLIST:checklist-7";

            service.markNotificationAsRead(userId, notificationId);

            then(notificationReadRepository).should().save(argThat(read ->
                    read.getType() == NotificationType.CHECKLIST
                    && read.getRelatedId().equals("checklist-7")
            ));
        }

        @Test
        @DisplayName("콜론이 2개 미만인 ID이면 NOTIFICATION_INVALID_ID_FORMAT 예외를 던진다")
        void tooFewParts_throwsInvalidIdFormat() {
            String invalidId = "SCHEDULE:only-two";

            assertThatThrownBy(() -> service.markNotificationAsRead(userId, invalidId))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.NOTIFICATION_INVALID_ID_FORMAT));

            then(notificationReadRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("콜론이 없는 ID이면 NOTIFICATION_INVALID_ID_FORMAT 예외를 던진다")
        void noColonId_throwsInvalidIdFormat() {
            String invalidId = "justaplainstring";

            assertThatThrownBy(() -> service.markNotificationAsRead(userId, invalidId))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.NOTIFICATION_INVALID_ID_FORMAT));

            then(notificationReadRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("빈 문자열 ID이면 NOTIFICATION_INVALID_ID_FORMAT 예외를 던진다")
        void emptyId_throwsInvalidIdFormat() {
            assertThatThrownBy(() -> service.markNotificationAsRead(userId, ""))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.NOTIFICATION_INVALID_ID_FORMAT));
        }

        @Test
        @DisplayName("존재하지 않는 타입 문자열이면 IllegalArgumentException을 던진다")
        void unknownType_throwsIllegalArgument() {
            String invalidId = userId + ":UNKNOWN_TYPE:relatedId";

            assertThatThrownBy(() -> service.markNotificationAsRead(userId, invalidId))
                    .isInstanceOf(IllegalArgumentException.class);

            then(notificationReadRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("저장되는 NotificationRead의 id는 전달받은 notificationId와 동일하다")
        void savedRead_idMatchesNotificationId() {
            String notificationId = userId + ":SCHEDULE:sched-001";

            service.markNotificationAsRead(userId, notificationId);

            then(notificationReadRepository).should().save(argThat(read ->
                    read.getId().equals(notificationId)
            ));
        }
    }

    // ──────────────────────────────────────────────
    // isNotificationRead
    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("isNotificationRead")
    class IsNotificationRead {

        @Test
        @DisplayName("레포지토리가 true를 반환하면 true를 반환한다")
        void repositoryReturnsTrue_returnsTrue() {
            String notificationId = userId + ":SCHEDULE:sched-1";
            given(notificationReadRepository.existsByIdAndUserId(notificationId, userId)).willReturn(true);

            assertThat(service.isNotificationRead(userId, notificationId)).isTrue();
        }

        @Test
        @DisplayName("레포지토리가 false를 반환하면 false를 반환한다")
        void repositoryReturnsFalse_returnsFalse() {
            String notificationId = userId + ":PAYMENT:pay-2";
            given(notificationReadRepository.existsByIdAndUserId(notificationId, userId)).willReturn(false);

            assertThat(service.isNotificationRead(userId, notificationId)).isFalse();
        }

        @Test
        @DisplayName("조회 시 notificationId와 userId를 그대로 레포지토리에 전달한다")
        void passesCorrectParamsToRepository() {
            String notificationId = userId + ":CHECKLIST:check-3";
            given(notificationReadRepository.existsByIdAndUserId(any(), any())).willReturn(false);

            service.isNotificationRead(userId, notificationId);

            then(notificationReadRepository).should()
                    .existsByIdAndUserId(eq(notificationId), eq(userId));
        }

        @Test
        @DisplayName("다른 userId로 조회하면 레포지토리에 해당 userId가 전달된다")
        void differentUserId_passedCorrectly() {
            UUID anotherUserId = UUID.randomUUID();
            String notificationId = userId + ":SCHEDULE:sched-99";
            given(notificationReadRepository.existsByIdAndUserId(any(), eq(anotherUserId))).willReturn(false);

            service.isNotificationRead(anotherUserId, notificationId);

            then(notificationReadRepository).should()
                    .existsByIdAndUserId(eq(notificationId), eq(anotherUserId));
        }
    }
}
