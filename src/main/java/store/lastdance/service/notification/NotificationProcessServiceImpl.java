package store.lastdance.service.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import store.lastdance.domain.notification.NotificationSetting;
import store.lastdance.domain.notification.NotificationType;
import store.lastdance.domain.user.User;
import store.lastdance.repository.notification.NotificationSettingRepository;
import store.lastdance.service.notification.checker.NotificationChecker;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
public class NotificationProcessServiceImpl implements NotificationProcessService {

    private final NotificationSettingRepository settingRepository;
    private final Map<NotificationType, NotificationChecker> checkerMap;

    public NotificationProcessServiceImpl(
            NotificationSettingRepository settingRepository,
            List<NotificationChecker> checkers) {
        this.settingRepository = settingRepository;
        this.checkerMap = checkers.stream()
                .collect(Collectors.toMap(NotificationChecker::getType, Function.identity()));
    }

    @Override
    public List<NotificationSetting> findAllActiveSettings() {
        return settingRepository.findAllActiveWithUser();
    }

    @Override
    public void checkAndSendNotifications(User user, NotificationSetting setting) {
        LocalDateTime now = LocalDateTime.now();

        checkerMap.forEach((type, checker) -> {
            if (!setting.isNotificationEnabled(type)) return;
            try {
                checker.check(user, setting, now);
            } catch (Exception e) {
                log.error("Checker 실행 실패: userId={}, type={}, error={}",
                        user.getUserId(), type, e.getMessage());
            }
        });
    }
}
