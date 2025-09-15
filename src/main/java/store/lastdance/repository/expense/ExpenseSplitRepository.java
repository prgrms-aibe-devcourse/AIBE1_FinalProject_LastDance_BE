package store.lastdance.repository.expense;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import store.lastdance.domain.expense.Expense;
import store.lastdance.domain.expense.ExpenseSplit;
import store.lastdance.domain.user.User;

import java.util.List;

public interface ExpenseSplitRepository extends JpaRepository<ExpenseSplit, Long> {

    List<ExpenseSplit> findByExpense(Expense expense);

    List<ExpenseSplit> findByExpenseIn(List<Expense> expenses);

    @Query("SELECT es FROM ExpenseSplit es WHERE es.user = :user " +
            "AND es.paid = false " +
            "AND es.createdAt BETWEEN :startDate AND :endDate " +
            "ORDER BY es.createdAt ASC")
    List<ExpenseSplit> findUnpaidSplitsByUserAndDate(@Param("user") User user,
                                                     @Param("startDate") java.time.LocalDateTime startDate,
                                                     @Param("endDate") java.time.LocalDateTime endDate);

    void deleteByExpense(Expense expense);
    
    @Modifying
    @Query("DELETE FROM ExpenseSplit es WHERE es.expense.group.groupId = :groupId")
    void deleteByGroupId(@Param("groupId") java.util.UUID groupId);

}