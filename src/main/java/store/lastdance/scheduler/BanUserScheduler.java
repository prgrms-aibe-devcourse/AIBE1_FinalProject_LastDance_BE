package store.lastdance.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import store.lastdance.domain.user.User;
import store.lastdance.domain.user.UserRole;
import store.lastdance.repository.user.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class BanUserScheduler {

    private final UserRepository userRepository;

    @Scheduled(fixedRate = 60000)
    public void unbanExpiredUsers() {
        LocalDateTime now = LocalDateTime.now();
        List<User> expiredBannedUsers = userRepository.findByIsBannedTrueAndBanEndDateBefore(now);

        for (User user : expiredBannedUsers) {
            user.unban();
            userRepository.save(user);

            // 알림 발송..?
        }
    }
}
