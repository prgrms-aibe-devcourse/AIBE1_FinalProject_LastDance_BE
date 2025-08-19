package store.lastdance.repository.analysis;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import store.lastdance.domain.analysis.ExpenseAnalysisHistory;
import store.lastdance.domain.user.User;
import store.lastdance.dto.admin.AdminExpenseAnalyzerHistoryDTO;
import store.lastdance.dto.admin.ExpenseAnalyzerFeedbackStatsDTO;

import java.time.LocalDateTime;
import java.util.List;

public interface ExpenseAnalysisHistoryRepository extends JpaRepository<ExpenseAnalysisHistory, Long>,
        JpaSpecificationExecutor<ExpenseAnalysisHistory> {

    @EntityGraph(attributePaths = "user")
    Page<ExpenseAnalysisHistory> findAll(Specification<ExpenseAnalysisHistory> spec, Pageable pageable);

    @Query("SELECT new store.lastdance.dto.admin.AdminExpenseAnalyzerHistoryDTO(" +
           "eah.id, eah.user.email, eah.user.nickname, eah.createdAt, eah.up, eah.down) " +
           "FROM ExpenseAnalysisHistory eah JOIN eah.user u")
    Page<AdminExpenseAnalyzerHistoryDTO> findHistoryProjection(Specification<ExpenseAnalysisHistory> spec, Pageable pageable);

    // 특정 사용자의 모든 분석 내역을 최신순으로 조회
    List<ExpenseAnalysisHistory> findByUserOrderByCreatedAtDesc(User user);

    @EntityGraph(attributePaths = "user")
    Page<ExpenseAnalysisHistory> findByUser(User user, Pageable pageable);

    List<ExpenseAnalysisHistory> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);


    @Query("SELECT new store.lastdance.dto.admin.ExpenseAnalyzerFeedbackStatsDTO(" +
            "   COALESCE(SUM(CASE WHEN e.up = true OR e.down = true THEN 1 ELSE 0 END), 0L), " +
            "   COALESCE(SUM(CASE WHEN e.up = true THEN 1 ELSE 0 END), 0L), " +
            "   COALESCE(SUM(CASE WHEN e.down = true THEN 1 ELSE 0 END), 0L), " +
            "   0.0, " + // satisfactionRate will be calculated in service layer
            "   null) " + // trends will be calculated in service layer
            "FROM ExpenseAnalysisHistory e")
    ExpenseAnalyzerFeedbackStatsDTO getFeedbackStatsSummary();

    @Query("SELECT new store.lastdance.dto.admin.ExpenseAnalyzerFeedbackStatsDTO(" +
            "   COALESCE(SUM(CASE WHEN e.up = true OR e.down = true THEN 1 ELSE 0 END), 0L), " +
            "   COALESCE(SUM(CASE WHEN e.up = true THEN 1 ELSE 0 END), 0L), " +
            "   COALESCE(SUM(CASE WHEN e.down = true THEN 1 ELSE 0 END), 0L), " +
            "   0.0, " + // satisfactionRate will be calculated in service layer
            "   null) " + // trends will be calculated in service layer
            "FROM ExpenseAnalysisHistory e WHERE e.createdAt BETWEEN :startDate AND :endDate")
    ExpenseAnalyzerFeedbackStatsDTO getFeedbackStatsSummaryBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
@Query("SELECT new store.lastdance.dto.admin.FeedbackTrendDTO(" +
            "   YEAR(e.createdAt), " +
            "   MONTH(e.createdAt), " +
            "   DAY(e.createdAt), " +
            "   COUNT(e), " +
            "   COALESCE(SUM(CASE WHEN e.up = true THEN 1 ELSE 0 END), 0L), " +
            "   COALESCE(SUM(CASE WHEN e.down = true THEN 1 ELSE 0 END), 0L)) " +
            "FROM ExpenseAnalysisHistory e " +
            "WHERE e.createdAt BETWEEN :startDate AND :endDate " +
            "GROUP BY YEAR(e.createdAt), MONTH(e.createdAt), DAY(e.createdAt) " +
            "ORDER BY YEAR(e.createdAt), MONTH(e.createdAt), DAY(e.createdAt)")
    List<store.lastdance.dto.admin.FeedbackTrendDTO> findFeedbackTrendsBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT new store.lastdance.dto.admin.FeedbackTrendDTO(" +
            "   YEAR(e.createdAt), " +
            "   MONTH(e.createdAt), " +
            "   DAY(e.createdAt), " +
            "   COUNT(e), " +
            "   COALESCE(SUM(CASE WHEN e.up = true THEN 1 ELSE 0 END), 0L), " +
            "   COALESCE(SUM(CASE WHEN e.down = true THEN 1 ELSE 0 END), 0L)) " +
            "FROM ExpenseAnalysisHistory e " +
            "GROUP BY YEAR(e.createdAt), MONTH(e.createdAt), DAY(e.createdAt) " +
            "ORDER BY YEAR(e.createdAt), MONTH(e.createdAt), DAY(e.createdAt)")
    List<store.lastdance.dto.admin.FeedbackTrendDTO> findFeedbackTrends();
}
