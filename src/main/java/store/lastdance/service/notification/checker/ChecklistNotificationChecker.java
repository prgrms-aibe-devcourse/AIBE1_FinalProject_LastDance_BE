package store.lastdance.service.notification.checker;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import store.lastdance.domain.checklist.Checklist;
import store.lastdance.domain.notification.NotificationSetting;
import store.lastdance.domain.notification.NotificationType;
import store.lastdance.domain.user.User;
import store.lastdance.repository.checklist.ChecklistRepository;
import store.lastdance.service.notification.NotificationSender;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChecklistNotificationChecker implements NotificationChecker {

    private final ChecklistRepository checklistRepository;
    private final NotificationSender notificationSender;

    @Override
    public NotificationType getType() {
        return NotificationType.CHECKLIST;
    }

    @Override
    public void check(User user, NotificationSetting setting, LocalDateTime now) {
        LocalDateTime startOfDay = now.toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);

        List<Checklist> checklists = checklistRepository.findByUserIdAndDueDateBetweenAndIsCompletedFalse(
                user.getUserId(), startOfDay, endOfDay);

        for (Checklist checklist : checklists) {
            String title = checklist.getGroup() != null
                    ? "[" + checklist.getGroup().getGroupName() + " 할일] " + checklist.getTitle()
                    : "[개인 할일] " + checklist.getTitle();
            notificationSender.sendIfNotCached(
                    user, setting, NotificationType.CHECKLIST,
                    title, "오늘이 마감일입니다.",
                    checklist.getChecklistId().toString());
        }
    }
}
