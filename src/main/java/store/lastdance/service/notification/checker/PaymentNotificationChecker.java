package store.lastdance.service.notification.checker;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import store.lastdance.domain.expense.ExpenseSplit;
import store.lastdance.domain.notification.NotificationSetting;
import store.lastdance.domain.notification.NotificationType;
import store.lastdance.domain.user.User;
import store.lastdance.repository.expense.ExpenseSplitRepository;
import store.lastdance.service.notification.NotificationSender;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentNotificationChecker implements NotificationChecker {

    private final ExpenseSplitRepository expenseSplitRepository;
    private final NotificationSender notificationSender;

    @Override
    public NotificationType getType() {
        return NotificationType.PAYMENT;
    }

    @Override
    public void check(User user, NotificationSetting setting, LocalDateTime now) {
        LocalDateTime startOfDay = now.toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);

        List<ExpenseSplit> splits = expenseSplitRepository.findUnpaidSplitsByUserAndDate(user, startOfDay, endOfDay);

        for (ExpenseSplit split : splits) {
            notificationSender.sendIfNotCached(
                    user, setting, NotificationType.PAYMENT,
                    resolveTitle(split), "새로운 정산 요청이 있습니다.",
                    split.getSplitId().toString());
        }
    }

    private String resolveTitle(ExpenseSplit split) {
        if (split.getExpense() == null) {
            return "지출 (분담금: " + split.getAmount() + "원)";
        }
        String base = split.getExpense().getGroup() != null
                ? "[" + split.getExpense().getGroup().getGroupName() + " 정산] " + split.getExpense().getTitle()
                : "[개인 정산] " + split.getExpense().getTitle();
        return base + " (분담금: " + split.getAmount() + "원)";
    }
}
