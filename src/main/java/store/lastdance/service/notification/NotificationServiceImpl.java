package store.lastdance.service.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import store.lastdance.domain.calendar.Calendar;
import store.lastdance.domain.notification.NotificationCache;
import store.lastdance.domain.notification.NotificationType;
import store.lastdance.domain.user.User;
import store.lastdance.repository.notification.NotificationCacheRepository;
import store.lastdance.repository.user.UserRepository;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationCacheRepository notificationCacheRepository;
    private final MailService mailService;
    private final UserRepository userRepository;

    @Override
    public void sendEmailScheduleReminder(Calendar calendar) {
        UUID userId = calendar.getUserId();
        User user = userRepository.findById(userId).orElse(null);

        if (user == null || user.getEmail() == null) {
            log.warn("사용자 정보 또는 이메일이 없음: userId={}", userId);
            return;
        }

        try {
            String subject = "[알림] 15분 후 일정: " + calendar.getTitle();
            String content = "안녕하세요!\n\n'" + calendar.getTitle() + "' 일정이 곧 시작됩니다.\n"
                    + "시작 시간: " + calendar.getStartDate().toString().replace("T", " ") + "\n\n감사합니다.";

            // 이메일 전송
            mailService.sendSimpleMail(user.getEmail(), subject, content);

            // Redis에 알림 기록 저장 (30일 TTL)
            NotificationCache notificationCache = NotificationCache.create(
                    userId,
                    NotificationType.SCHEDULE,
                    subject,
                    content,
                    calendar.getCalendarId().toString()
            );
            notificationCacheRepository.save(notificationCache);
            
            log.info("일정 알림 전송 완료 (Redis 저장): userId={}, calendarId={}, email={}", 
                    userId, calendar.getCalendarId(), user.getEmail());
            
        } catch (Exception e) {
            log.error("일정 알림 전송 실패: userId={}, calendarId={}, error={}", 
                    userId, calendar.getCalendarId(), e.getMessage());
        }
    }

    @Override
    public boolean isAlreadyNotified(Long calendarId, UUID userId) {
        return notificationCacheRepository.isAlreadyNotified(
                userId, 
                NotificationType.SCHEDULE, 
                calendarId.toString()
        );
    }

    @Override
    public List<NotificationCache> getUserNotifications(UUID userId) {
        try {
            return notificationCacheRepository.findByUserId(userId);
        } catch (Exception e) {
            log.error("사용자 알림 조회 실패: userId={}, error={}", userId, e.getMessage());
            return List.of();
        }
    }

    @Override
    public List<NotificationCache> getUserNotificationsByType(UUID userId, String type) {
        try {
            NotificationType notificationType = NotificationType.valueOf(type.toUpperCase());
            return notificationCacheRepository.findByUserIdAndType(userId, notificationType);
        } catch (IllegalArgumentException e) {
            log.warn("잘못된 알림 타입: type={}", type);
            return List.of();
        } catch (Exception e) {
            log.error("사용자 알림 타입별 조회 실패: userId={}, type={}, error={}", userId, type, e.getMessage());
            return List.of();
        }
    }
}
