package store.lastdance.repository.expense;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import store.lastdance.domain.expense.Expense;
import store.lastdance.domain.expense.ExpenseCategory;
import store.lastdance.domain.group.Group;
import store.lastdance.domain.user.User;
import store.lastdance.dto.expense.CategoryStatsProjection;
import store.lastdance.dto.expense.ShareCategoryStatsProjection;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ExpenseRepositoryCustom {

    Optional<Expense> findByExpenseIdWithPermission(Long expenseId, User user);

    List<Expense> findPersonalExpensesByMonthRange(
            User user,
            LocalDate startDate,
            LocalDate endDate,
            ExpenseCategory category
    );

    Page<Expense> findPersonalExpensesForCombined(
            User user,
            LocalDate startDate,
            LocalDate endDate,
            ExpenseCategory category,
            String search,
            Pageable pageable
    );

    List<Expense> findPersonalAndShareExpensesByDateRange(
            User user,
            LocalDate startDate,
            LocalDate endDate
    );

    List<Expense> findGroupExpensesByMonthRange(
            Group group,
            LocalDate startDate,
            LocalDate endDate,
            ExpenseCategory category
    );

    Page<Expense> findGroupExpensesByMonthWithPaging(
            Group group,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable
    );

    List<CategoryStatsProjection> getGroupExpenseCategoryStats(
            Group group,
            LocalDate startDate,
            LocalDate endDate,
            ExpenseCategory category,
            String search
    );

    Optional<Expense> findTopGroupExpense(
            Group group,
            LocalDate startDate,
            LocalDate endDate,
            ExpenseCategory category,
            String search
    );

    List<Expense> findShareExpensesByUserAndMonth(
            User user,
            LocalDate startDate,
            LocalDate endDate
    );

    Page<Expense> findShareExpensesByGroupAndMonthWithPagingFiltered(
            User user,
            Group group,
            LocalDate startDate,
            LocalDate endDate,
            ExpenseCategory category,
            String search,
            Pageable pageable
    );

    Page<Expense> findShareExpensesForCombined(
            User user,
            LocalDate startDate,
            LocalDate endDate,
            ExpenseCategory category,
            String search,
            Pageable pageable
    );

    List<ShareCategoryStatsProjection> getShareExpenseCategoryStats(
            User user,
            Group group,
            LocalDate startDate,
            LocalDate endDate,
            ExpenseCategory category,
            String search
    );

    Optional<Expense> findTopShareExpense(
            User user,
            Group group,
            LocalDate startDate,
            LocalDate endDate,
            ExpenseCategory category,
            String search
    );

    Page<Expense> findCombinedExpenseForUser(
            User user,
            LocalDate startDate,
            LocalDate endDate,
            ExpenseCategory category,
            String search,
            Pageable pageable
    );

    Page<Expense> findGroupExpensesBySearchAndMonthWithPaging(
            Group group,
            String search,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable
    );

    Page<Expense> findGroupExpensesByCategoryAndMonthWithPaging(
            Group group,
            ExpenseCategory category,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable
    );

    List<CategoryStatsProjection> getCombinedExpenseCategoryStats(
            User user,
            LocalDate startDate,
            LocalDate endDate,
            ExpenseCategory category,
            String search
    );

    Optional<Expense> findTopCombinedExpense(
            User user,
            LocalDate startDate,
            LocalDate endDate,
            ExpenseCategory category,
            String search
    );
}
