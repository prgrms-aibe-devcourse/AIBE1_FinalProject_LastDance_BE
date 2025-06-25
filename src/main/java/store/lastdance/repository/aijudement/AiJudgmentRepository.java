// Repository Interface
package store.lastdance.repository.aijudement;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import store.lastdance.domain.aijudgment.AiJudgment;

import java.util.UUID;

@Repository
public interface AiJudgmentRepository extends JpaRepository<AiJudgment, UUID> {
}