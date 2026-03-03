package store.lastdance.repository.expense;

import org.springframework.data.jpa.repository.JpaRepository;
import store.lastdance.domain.expense.Expense;
import store.lastdance.domain.expense.ExpenseSplit;

import java.util.List;

public interface ExpenseSplitRepository extends JpaRepository<ExpenseSplit, Long>, ExpenseSplitRepositoryCustom {

    List<ExpenseSplit> findByExpense(Expense expense);

    List<ExpenseSplit> findByExpenseIn(List<Expense> expenses);

    void deleteByExpense(Expense expense);


}