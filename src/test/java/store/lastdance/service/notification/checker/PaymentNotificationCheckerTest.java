package store.lastdance.service.notification.checker;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import store.lastdance.domain.expense.Expense;
import store.lastdance.domain.expense.ExpenseSplit;
import store.lastdance.domain.group.Group;
import store.lastdance.domain.notification.NotificationSetting;
import store.lastdance.domain.notification.NotificationType;
import store.lastdance.domain.user.User;
import store.lastdance.repository.expense.ExpenseSplitRepository;
import store.lastdance.service.notification.NotificationSender;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentNotificationChecker 테스트")
class PaymentNotificationCheckerTest {

    @Mock private ExpenseSplitRepository expenseSplitRepository;
    @Mock private NotificationSender notificationSender;

    @InjectMocks
    private PaymentNotificationChecker checker;

    private UUID userId;
    private User user;
    private NotificationSetting setting;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = mock(User.class);
        lenient().when(user.getUserId()).thenReturn(userId);

        setting = NotificationSetting.builder().userId(userId).build();
        setting.updateSSEEnabled(true);
        setting.updatePaymentReminder(true);

        now = LocalDate.now().atTime(10, 0);
    }

    // ──────────────────────────────────────────────
    // getType
    // ──────────────────────────────────────────────
    @Test
    @DisplayName("getType은 PAYMENT를 반환한다")
    void getType_returnsPayment() {
        assertThat(checker.getType()).isEqualTo(NotificationType.PAYMENT);
    }

    // ──────────────────────────────────────────────
    // check - 조회 범위
    // ──────────────────────────────────────────────
    @Test
    @DisplayName("조회 범위는 당일 자정부터 다음날 자정까지이다")
    void queryRange_isFullDay() {
        given(expenseSplitRepository.findUnpaidSplitsByUserAndDate(eq(user), any(), any()))
                .willReturn(List.of());

        checker.check(user, setting, now);

        LocalDateTime expectedStart = now.toLocalDate().atStartOfDay();
        LocalDateTime expectedEnd = expectedStart.plusDays(1);

        then(expenseSplitRepository).should().findUnpaidSplitsByUserAndDate(
                eq(user),
                eq(expectedStart),
                eq(expectedEnd)
        );
    }

    // ──────────────────────────────────────────────
    // check - 개인 정산
    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("개인 정산 알림")
    class PersonalPayment {

        @Test
        @DisplayName("개인 정산이 있으면 '[개인 정산]' 제목으로 sendIfNotCached를 호출한다")
        void personalSplit_sendsWithPersonalTitle() {
            ExpenseSplit split = mockSplit(1L, null, "회식비", new BigDecimal("30000"));
            given(expenseSplitRepository.findUnpaidSplitsByUserAndDate(eq(user), any(), any()))
                    .willReturn(List.of(split));

            checker.check(user, setting, now);

            then(notificationSender).should().sendIfNotCached(
                    eq(user), eq(setting), eq(NotificationType.PAYMENT),
                    contains("[개인 정산]"), eq("새로운 정산 요청이 있습니다."), eq("1"));
        }

        @Test
        @DisplayName("정산 제목에 분담금 금액이 포함된다")
        void personalSplit_titleContainsAmount() {
            ExpenseSplit split = mockSplit(1L, null, "카페", new BigDecimal("15000"));
            given(expenseSplitRepository.findUnpaidSplitsByUserAndDate(eq(user), any(), any()))
                    .willReturn(List.of(split));

            checker.check(user, setting, now);

            then(notificationSender).should().sendIfNotCached(
                    eq(user), eq(setting), eq(NotificationType.PAYMENT),
                    contains("15000"), anyString(), anyString());
        }

        @Test
        @DisplayName("미납 정산이 없으면 sendIfNotCached를 호출하지 않는다")
        void noSplits_noop() {
            given(expenseSplitRepository.findUnpaidSplitsByUserAndDate(eq(user), any(), any()))
                    .willReturn(List.of());

            checker.check(user, setting, now);

            then(notificationSender).should(never()).sendIfNotCached(any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("미납 정산이 여러 개면 각각 sendIfNotCached를 호출한다")
        void multipleSplits_sendsEach() {
            ExpenseSplit s1 = mockSplit(1L, null, "점심", new BigDecimal("10000"));
            ExpenseSplit s2 = mockSplit(2L, null, "저녁", new BigDecimal("20000"));
            given(expenseSplitRepository.findUnpaidSplitsByUserAndDate(eq(user), any(), any()))
                    .willReturn(List.of(s1, s2));

            checker.check(user, setting, now);

            then(notificationSender).should(times(2))
                    .sendIfNotCached(eq(user), eq(setting), eq(NotificationType.PAYMENT),
                            anyString(), anyString(), anyString());
        }
    }

    // ──────────────────────────────────────────────
    // check - 그룹 정산
    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("그룹 정산 알림")
    class GroupPayment {

        @Test
        @DisplayName("그룹 정산이면 '[그룹명 정산]' 제목으로 sendIfNotCached를 호출한다")
        void groupSplit_sendsWithGroupName() {
            Group group = mockGroup("여행팀");
            ExpenseSplit split = mockSplit(1L, group, "항공권", new BigDecimal("150000"));
            given(expenseSplitRepository.findUnpaidSplitsByUserAndDate(eq(user), any(), any()))
                    .willReturn(List.of(split));

            checker.check(user, setting, now);

            then(notificationSender).should().sendIfNotCached(
                    eq(user), eq(setting), eq(NotificationType.PAYMENT),
                    contains("[여행팀 정산]"), anyString(), anyString());
        }

        @Test
        @DisplayName("그룹 정산 제목에도 분담금이 포함된다")
        void groupSplit_titleContainsAmount() {
            Group group = mockGroup("우리집");
            ExpenseSplit split = mockSplit(1L, group, "마트", new BigDecimal("50000"));
            given(expenseSplitRepository.findUnpaidSplitsByUserAndDate(eq(user), any(), any()))
                    .willReturn(List.of(split));

            checker.check(user, setting, now);

            then(notificationSender).should().sendIfNotCached(
                    eq(user), eq(setting), eq(NotificationType.PAYMENT),
                    contains("50000"), anyString(), anyString());
        }
    }

    // ──────────────────────────────────────────────
    // check - expense null 처리
    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("expense null 처리")
    class NullExpense {

        @Test
        @DisplayName("expense가 null이면 '지출'로 대체한 제목으로 sendIfNotCached를 호출한다")
        void nullExpense_fallbackTitle() {
            ExpenseSplit split = mock(ExpenseSplit.class);
            given(split.getSplitId()).willReturn(1L);
            given(split.getAmount()).willReturn(new BigDecimal("5000"));
            given(split.getExpense()).willReturn(null);

            given(expenseSplitRepository.findUnpaidSplitsByUserAndDate(eq(user), any(), any()))
                    .willReturn(List.of(split));

            checker.check(user, setting, now);

            then(notificationSender).should().sendIfNotCached(
                    eq(user), eq(setting), eq(NotificationType.PAYMENT),
                    contains("지출"), anyString(), anyString());
        }
    }

    // ──────────────────────────────────────────────
    // helpers
    // ──────────────────────────────────────────────

    private ExpenseSplit mockSplit(Long id, Group group, String expenseTitle, BigDecimal amount) {
        Expense expense = mock(Expense.class);
        given(expense.getTitle()).willReturn(expenseTitle);
        given(expense.getGroup()).willReturn(group);

        ExpenseSplit split = mock(ExpenseSplit.class);
        given(split.getSplitId()).willReturn(id);
        given(split.getAmount()).willReturn(amount);
        given(split.getExpense()).willReturn(expense);
        return split;
    }

    private Group mockGroup(String groupName) {
        Group group = mock(Group.class);
        given(group.getGroupName()).willReturn(groupName);
        return group;
    }
}
