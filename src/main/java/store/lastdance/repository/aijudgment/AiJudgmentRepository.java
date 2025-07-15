package store.lastdance.repository.aijudgment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import store.lastdance.domain.aijudgment.AiJudgment;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AiJudgmentRepository extends JpaRepository<AiJudgment, UUID> {

    Page<AiJudgment> findAll(Specification<AiJudgment> spec, Pageable pageable);

    AiJudgment findByJudgmentId(UUID judgmentId);

    boolean existsByJudgmentId(UUID judgmentId);

    List<AiJudgment> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    List<AiJudgment> findByUserIdOrderByCreatedAtDesc(UUID userId);
}