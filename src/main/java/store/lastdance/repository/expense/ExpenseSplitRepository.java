package store.lastdance.repository.expense;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import store.lastdance.domain.expense.ExpenseSplit;

import java.util.List;
import java.util.UUID;

public interface ExpenseSplitRepository extends JpaRepository<ExpenseSplit, Long> {

    // 특정 지출의 모든 분담 정보
    List<ExpenseSplit> findByExpenseId(Long expenseId);

    // 사용자의 분담 정보 조회
    List<ExpenseSplit> findByUserId(UUID userId);

    // 특정 지출에서 특정 사용자의 분담 정보
    List<ExpenseSplit> findByExpenseIdAndUserId(Long expenseId, UUID userId);

    // 사용자의 미정산 분담금 조회
    @Query("SELECT es FROM ExpenseSplit es WHERE es.userId = :userId AND es.paid = false")
    List<ExpenseSplit> findUnpaidSplitsByUserId(@Param("userId") UUID userId);

    // 그룹의 미정산 분담금 조회
    @Query("SELECT es FROM ExpenseSplit es JOIN es.expense e " +
            "WHERE e.groupId = :groupId AND es.paid = false")
    List<ExpenseSplit> findUnpaidSplitsByGroupId(@Param("groupId") UUID groupId);

    // 월별 사용자 분담금 조회
    @Query("SELECT es FROM ExpenseSplit es JOIN es.expense e " +
            "WHERE es.userId = :userId " +
            "AND YEAR(e.expenseDate) = :year AND MONTH(e.expenseDate) = :month")
    List<ExpenseSplit> findSplitsByUserAndMonth(
            @Param("userId") UUID userId,
            @Param("year") int year,
            @Param("month") int month
    );

    // 특정 날짜에 생성된 사용자의 미정산 분담금 조회 (알림용)
    @Query("SELECT es FROM ExpenseSplit es WHERE es.userId = :userId " +
           "AND es.paid = false " +
           "AND es.createdAt BETWEEN :startDate AND :endDate " +
           "ORDER BY es.createdAt ASC")
    List<ExpenseSplit> findUnpaidSplitsByUserIdAndDate(@Param("userId") UUID userId,
                                                     @Param("startDate") java.time.LocalDateTime startDate,
                                                     @Param("endDate") java.time.LocalDateTime endDate);

    void deleteByExpenseId(Long expenseId);
}