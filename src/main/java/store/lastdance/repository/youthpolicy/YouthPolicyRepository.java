package store.lastdance.repository.youthpolicy;

import org.springframework.data.jpa.repository.JpaRepository;
import store.lastdance.domain.youthpolicy.YouthPolicy;

public interface YouthPolicyRepository extends JpaRepository<YouthPolicy, String> {
}
