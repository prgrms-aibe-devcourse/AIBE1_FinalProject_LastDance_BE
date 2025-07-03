package store.lastdance.scheduler.youthpolicy;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import store.lastdance.service.youthpolicy.YouthPolicyService;

@Component
@RequiredArgsConstructor
public class YouthPolicyScheduler {

    private final YouthPolicyService youthPolicyService;

    // 매일 자정에 실행
    @Scheduled(cron = "0 0 0 * * *")
    public void updateYouthPolicies() {
        youthPolicyService.syncPoliciesWithOpenApi();
    }
}
