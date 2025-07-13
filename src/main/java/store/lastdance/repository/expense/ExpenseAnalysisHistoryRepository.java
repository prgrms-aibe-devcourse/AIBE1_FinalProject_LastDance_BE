package store.lastdance.repository.expense;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import store.lastdance.domain.expense.ExpenseAnalysisHistory;
import store.lastdance.domain.user.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface ExpenseAnalysisHistoryRepository extends JpaRepository<ExpenseAnalysisHistory, Long> {

    // 특정 사용자의 모든 분석 내역을 최신순으로 조회
    List<ExpenseAnalysisHistory> findByUserOrderByCreatedAtDesc(User user);

    Page<ExpenseAnalysisHistory> findByUser(User user, Pageable pageable);

    List<ExpenseAnalysisHistory> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    Page<ExpenseAnalysisHistory> findAll(Specification<ExpenseAnalysisHistory> spec, Pageable pageable);
}