package store.lastdance.repository.expense;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import store.lastdance.domain.expense.Expense;
import store.lastdance.domain.expense.ExpenseSplit;
import store.lastdance.domain.user.User;

import java.util.List;

public interface ExpenseSplitRepository extends JpaRepository<ExpenseSplit, Long> {

    // 특정 지출의 모든 분담 정보
    List<ExpenseSplit> findByExpense(Expense expense);

    // 여러 지출의 모든 분담 정보를 한번에 조회 (N+1 해결용)
    List<ExpenseSplit> findByExpenseIn(List<Expense> expenses);

    // 특정 날짜에 생성된 사용자의 미정산 분담금 조회 (알림용)
    @Query("SELECT es FROM ExpenseSplit es WHERE es.user = :user " +
            "AND es.paid = false " +
            "AND es.createdAt BETWEEN :startDate AND :endDate " +
            "ORDER BY es.createdAt ASC")
    List<ExpenseSplit> findUnpaidSplitsByUserAndDate(@Param("user") User user,
                                                     @Param("startDate") java.time.LocalDateTime startDate,
                                                     @Param("endDate") java.time.LocalDateTime endDate);

    void deleteByExpense(Expense expense);

}