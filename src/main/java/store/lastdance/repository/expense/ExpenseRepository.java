package store.lastdance.repository.expense;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import store.lastdance.domain.expense.Expense;
import store.lastdance.domain.group.Group;

public interface ExpenseRepository extends JpaRepository<Expense, Long>, ExpenseRepositoryCustom {

    void deleteByOriginalExpense(Expense expense);

    @Modifying
    void deleteByGroup(Group group);

}