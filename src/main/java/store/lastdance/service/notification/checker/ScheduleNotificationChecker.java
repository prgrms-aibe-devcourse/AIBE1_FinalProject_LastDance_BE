package store.lastdance.service.notification.checker;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import store.lastdance.domain.calendar.Calendar;
import store.lastdance.domain.calendar.CalendarType;
import store.lastdance.domain.notification.NotificationSetting;
import store.lastdance.domain.notification.NotificationType;
import store.lastdance.domain.user.User;
import store.lastdance.repository.calendar.CalendarRepository;
import store.lastdance.repository.group.GroupRepository;
import store.lastdance.service.notification.NotificationSender;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleNotificationChecker implements NotificationChecker {

    private final CalendarRepository calendarRepository;
    private final GroupRepository groupRepository;
    private final NotificationSender notificationSender;

    @Override
    public NotificationType getType() {
        return NotificationType.SCHEDULE;
    }

    @Override
    public void check(User user, NotificationSetting setting, LocalDateTime now) {
        LocalDateTime reminderTime = now.plusMinutes(15);
        LocalDateTime startRange = reminderTime.minusMinutes(2);
        LocalDateTime endRange = reminderTime.plusMinutes(2);

        List<Calendar> allSchedules = new ArrayList<>();
        allSchedules.addAll(calendarRepository.findByUserIdAndStartTimeBetween(user.getUserId(), startRange, endRange));
        allSchedules.addAll(calendarRepository.findGroupCalendarsForUserInTimeRange(user.getUserId(), startRange, endRange));

        for (Calendar schedule : allSchedules) {
            notificationSender.sendIfNotCached(
                    user, setting, NotificationType.SCHEDULE,
                    resolveTitle(schedule), "15분 후 시작 예정입니다.",
                    schedule.getCalendarId().toString());
        }
    }

    private String resolveTitle(Calendar schedule) {
        if (schedule.getType() == CalendarType.GROUP && schedule.getGroup() != null) {
            String groupName = groupRepository.findGroupNameByGroupId(schedule.getGroup().getGroupId())
                    .orElse("그룹");
            return "[" + groupName + " 일정] " + schedule.getTitle();
        }
        return "[개인 일정] " + schedule.getTitle();
    }
}
