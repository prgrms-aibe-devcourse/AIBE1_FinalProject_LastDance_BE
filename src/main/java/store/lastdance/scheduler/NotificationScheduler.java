package store.lastdance.scheduler;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import store.lastdance.domain.calendar.Calendar;
import store.lastdance.domain.notification.NotificationSetting;
import store.lastdance.repository.calendar.CalendarRepository;
import store.lastdance.repository.notification.NotificationSettingRepository;
import store.lastdance.service.notification.NotificationService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class NotificationScheduler {

    private final CalendarRepository calendarRepository;
    private final NotificationSettingRepository settingRepository;
    private final NotificationService notificationService;

    @Scheduled(fixedRate = 60000) // 1분마다 확인
    public void sendScheduleReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime target = now.plusMinutes(15);

        List<Calendar> upcoming = calendarRepository.findByStartDateTimeBetween(now.plusSeconds(50), target);

        for (Calendar calendar : upcoming) {
            UUID userId = calendar.getUserId();

            NotificationSetting setting = settingRepository.findByUserId(userId);
            if (setting != null && Boolean.TRUE.equals(setting.getScheduleReminder())) {
                // 이미 알림을 보낸 일정인지 확인
                if (!notificationService.isAlreadyNotified(calendar.getCalendarId(), userId)) {
                    notificationService.sendEmailScheduleReminder(calendar);
                }
            }
        }
    }
}
