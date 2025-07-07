package store.lastdance.repository.youthpolicy;

import org.springframework.data.jpa.repository.JpaRepository;
import store.lastdance.domain.youthpolicy.YouthPolicy;

import java.util.Optional;

public interface YouthPolicyRepository extends JpaRepository<YouthPolicy, Long> {
    Optional<YouthPolicy> findByPlcyNo(String plcyNo);
}