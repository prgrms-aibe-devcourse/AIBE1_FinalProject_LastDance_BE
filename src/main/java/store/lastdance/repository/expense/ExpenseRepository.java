package store.lastdance.repository.expense;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import store.lastdance.domain.expense.Expense;
import store.lastdance.domain.expense.ExpenseCategory;
import store.lastdance.domain.group.Group;
import store.lastdance.domain.user.User;
import store.lastdance.dto.expense.BaseExpenseStats;
import store.lastdance.dto.expense.SimpleExpenseStats;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    void deleteByOriginalExpense(Expense expense);

    @Modifying
    void deleteByGroup(Group group);

    @Query("SELECT e FROM Expense e " +
            "LEFT JOIN FETCH e.originalExpense " +
            "LEFT JOIN FETCH e.group " +
            "WHERE e.user = :user " +
            "AND e.expenseType = 'SHARE' " +
            "AND YEAR(e.expenseDate) = :year AND MONTH(e.expenseDate) = :month " +
            "ORDER BY e.expenseDate DESC, e.createdAt DESC")
    List<Expense> findShareExpensesByUserAndMonth(
            @Param("user") User user,
            @Param("year") int year,
            @Param("month") int month
    );

    @Query("SELECT e FROM Expense e LEFT JOIN GroupMember gm ON e.group.groupId = gm.group.groupId " +
            "WHERE e.expenseId = :expenseId AND e.expenseType != 'SHARE' AND " +
            "(e.user = :user OR (e.expenseType = 'GROUP' AND gm.user = :user))")
    Optional<Expense> findByExpenseIdWithPermission(
            @Param("expenseId") Long expenseId,
            @Param("user") User user);

    @Query("SELECT e FROM Expense e WHERE e.user = :user " +
            "AND e.expenseType IN ('PERSONAL', 'SHARE') " +
            "AND e.expenseDate >= :startDate AND e.expenseDate <= :endDate " +
            "AND (:category IS NULL OR e.category = :category) " +
            "ORDER BY e.expenseDate DESC, e.createdAt DESC")
    List<Expense> findPersonalExpensesByMonthRange(
            @Param("user") User user,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("category") ExpenseCategory category
    );

    @Query("SELECT e FROM Expense e WHERE e.group = :group " +
            "AND e.expenseType = 'GROUP' " +
            "AND e.expenseDate >= :startDate AND e.expenseDate <= :endDate " +
            "AND (:category IS NULL OR e.category = :category) " +
            "ORDER BY e.expenseDate DESC, e.createdAt DESC")
    List<Expense> findGroupExpensesByMonthRange(
            @Param("group") Group group,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("category") ExpenseCategory category
    );

    @Query("SELECT e FROM Expense e WHERE e.group = :group " +
            "AND e.expenseType = 'GROUP' " +
            "AND YEAR(e.expenseDate) = :year AND MONTH(e.expenseDate) = :month " +
            "ORDER BY e.expenseDate DESC, e.createdAt DESC")
    Page<Expense> findGroupExpensesByMonthWithPaging(
            @Param("group") Group group,
            @Param("year") int year,
            @Param("month") int month,
            Pageable pageable
    );

    @Query("SELECT e FROM Expense e WHERE e.user = :user " +
            "AND e.group = :group " +
            "AND e.expenseType = 'SHARE' " +
            "AND YEAR(e.expenseDate) = :year AND MONTH(e.expenseDate) = :month " +
            "AND (:category IS NULL OR e.category = :category) " +
            "AND (:search IS NULL OR e.title LIKE %:search% OR e.memo LIKE %:search%) " +
            "ORDER BY e.expenseDate DESC, e.createdAt DESC")
    Page<Expense> findShareExpensesByGroupAndMonthWithPagingFiltered(
            @Param("user") User user,
            @Param("group") Group group,
            @Param("year") int year,
            @Param("month") int month,
            @Param("category") ExpenseCategory category,
            @Param("search") String search,
            Pageable pageable
    );

    @Query("SELECT e FROM Expense e WHERE e.user = :user " +
            "AND e.expenseType = 'PERSONAL' " +
            "AND YEAR(e.expenseDate) = :year AND MONTH(e.expenseDate) = :month " +
            "AND (:category IS NULL OR e.category = :category) " +
            "AND (:search IS NULL OR e.title LIKE %:search% OR e.memo LIKE %:search%) " +
            "ORDER BY e.expenseDate DESC, e.createdAt DESC")
    Page<Expense> findPersonalExpensesForCombined(
            @Param("user") User user,
            @Param("year") int year,
            @Param("month") int month,
            @Param("category") ExpenseCategory category,
            @Param("search") String search,
            Pageable pageable
    );

    @Query("SELECT e FROM Expense e " +
            "LEFT JOIN FETCH e.originalExpense " +
            "LEFT JOIN FETCH e.group " +
            "WHERE e.user = :user " +
            "AND e.expenseType = 'SHARE' " +
            "AND YEAR(e.expenseDate) = :year AND MONTH(e.expenseDate) = :month " +
            "AND (:category IS NULL OR e.category = :category) " +
            "AND (:search IS NULL OR e.title LIKE %:search% OR e.memo LIKE %:search%) " +
            "ORDER BY e.expenseDate DESC, e.createdAt DESC")
    Page<Expense> findShareExpensesForCombined(
            @Param("user") User user,
            @Param("year") int year,
            @Param("month") int month,
            @Param("category") ExpenseCategory category,
            @Param("search") String search,
            Pageable pageable
    );

    @Query("SELECT e FROM Expense e WHERE e.group = :group " +
            "AND YEAR(e.expenseDate) = :year AND MONTH(e.expenseDate) = :month " +
            "AND e.expenseType = 'GROUP' " +
            "AND (:search IS NULL OR e.title LIKE %:search% OR e.memo LIKE %:search%)")
    Page<Expense> findGroupExpensesBySearchAndMonthWithPaging(
            @Param("group") Group group,
            @Param("search") String search,
            @Param("year") int year,
            @Param("month") int month,
            Pageable pageable
    );

    @Query("SELECT e FROM Expense e WHERE e.group = :group " +
            "AND YEAR(e.expenseDate) = :year AND MONTH(e.expenseDate) = :month " +
            "AND e.expenseType = 'GROUP' AND e.category = :category")
    Page<Expense> findGroupExpensesByCategoryAndMonthWithPaging(
            @Param("group") Group group,
            @Param("category") ExpenseCategory category,
            @Param("year") int year,
            @Param("month") int month,
            Pageable pageable
    );

    @Query("SELECT e FROM Expense e WHERE e.user = :user " +
            "AND e.expenseType IN ('PERSONAL', 'SHARE') " +
            "AND e.expenseDate BETWEEN :startDate AND :endDate " +
            "ORDER BY e.expenseDate ASC ")
    List<Expense> findPersonalAndShareExpensesByDateRange(
            @Param("user") User user,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("""
                SELECT new store.lastdance.dto.expense.SimpleExpenseStats(
                    SUM(e.originalExpense.amount),
                    SUM(e.amount),
                    COUNT(e.expenseId),
                    MAX(e.amount)
                )
                FROM Expense e
                WHERE e.user = :user AND e.group = :group AND e.expenseType = 'SHARE'
                    AND FUNCTION('YEAR', e.expenseDate) = :year
                    AND FUNCTION('MONTH', e.expenseDate) = :month
                    AND (:category IS NULL OR e.originalExpense.category = :category)
                    AND (:search IS NULL OR LOWER(e.originalExpense.title) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    SimpleExpenseStats getShareExpenseBaseStats(
            @Param("user") User user,
            @Param("group") Group group,
            @Param("year") int year,
            @Param("month") int month,
            @Param("category") ExpenseCategory category,
            @Param("search") String search
    );

    @Query("""
                    SELECT e.originalExpense.category as category,
                            SUM(e.amount) as totalAmount,
                            COUNT(e.expenseId) as count
                    FROM Expense e
                    WHERE e.user = :user AND e.group = :group AND e.expenseType = 'SHARE'
                        AND FUNCTION('YEAR', e.expenseDate) = :year
                        AND FUNCTION('MONTH', e.expenseDate) = :month
                        AND (:category IS NULL OR e.originalExpense.category = :category)
                        AND (:search IS NULL OR LOWER(e.originalExpense.title) LIKE LOWER(CONCAT('%', :search, '%')))
                    GROUP BY e.originalExpense.category
            """)
    List<CategoryStatsProjection> getShareExpenseCategoryStats(
            @Param("user") User user,
            @Param("group") Group group,
            @Param("year") int year,
            @Param("month") int month,
            @Param("category") ExpenseCategory category,
            @Param("search") String search
    );

    @Query("""
                    SELECT e
                    FROM Expense e
                    WHERE e.user = :user AND e.group = :group AND e.expenseType = 'SHARE'
                        AND e.amount = :maxAmount
                        AND FUNCTION('YEAR', e.expenseDate) = :year
                        AND FUNCTION('MONTH', e.expenseDate) = :month
                        AND (:category IS NULL OR e.originalExpense.category = :category)
                        AND (:search IS NULL OR LOWER(e.originalExpense.title) LIKE LOWER(CONCAT('%', :search, '%')))
                    ORDER BY e.createdAt DESC
            """)
    Optional<Expense> findTopShareExpenseWithMaxAmount(
            @Param("user") User user,
            @Param("group") Group group,
            @Param("year") int year,
            @Param("month") int month,
            @Param("category") ExpenseCategory category,
            @Param("search") String search,
            @Param("maxAmount") BigDecimal maxAmount
    );

    @Query("""
            SELECT new store.lastdance.dto.expense.BaseExpenseStats(
                SUM(e.amount),
                COUNT(e.expenseId),
                MAX(e.amount)
            )
            FROM Expense e
            WHERE e.group = :group AND e.expenseType = 'GROUP'
                AND FUNCTION('YEAR', e.expenseDate) = :year
                AND FUNCTION('MONTH', e.expenseDate) = :month
                AND (:category IS NULL OR e.category = :category)
                AND (:search IS NULL OR LOWER(e.title) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    BaseExpenseStats getGroupExpenseBaseStats(
            @Param("group") Group group,
            @Param("year") int year,
            @Param("month") int month,
            @Param("category") ExpenseCategory category,
            @Param("search") String search
    );

    @Query("""
            SELECT e.category as category,
                    SUM(e.amount) as totalAmount,
                    COUNT(e.expenseId) as count
            FROM Expense e
            WHERE e.group = :group AND e.expenseType = 'GROUP'
                AND FUNCTION('YEAR', e.expenseDate) = :year
                AND FUNCTION('MONTH', e.expenseDate) = :month
                AND (:category IS NULL OR e.category = :category)
                AND (:search IS NULL OR LOWER(e.title) LIKE LOWER(CONCAT('%', :search, '%')))
            GROUP BY e.category
            """)
    List<CategoryStatsProjection> getGroupExpenseCategoryStats(
            @Param("group") Group group,
            @Param("year") int year,
            @Param("month") int month,
            @Param("category") ExpenseCategory category,
            @Param("search") String search
    );

    @Query("""
            SELECT e
            FROM Expense e
            WHERE e.group = :group AND e.expenseType = 'GROUP'
                AND e.amount = :maxAmount
                AND FUNCTION('YEAR', e.expenseDate) = :year
                AND FUNCTION('MONTH', e.expenseDate) = :month
                AND (:category IS NULL OR e.category = :category)
                AND (:search IS NULL OR LOWER(e.title) LIKE LOWER(CONCAT('%', :search, '%')))
            ORDER BY e.createdAt DESC
            """)
    Optional<Expense> findTopGroupExpenseWithMaxAmount(
            @Param("group") Group group,
            @Param("year") int year,
            @Param("month") int month,
            @Param("category") ExpenseCategory category,
            @Param("search") String search,
            @Param("maxAmount") BigDecimal maxAmount
    );

    @Query(value = """
            SELECT e
            FROM Expense e
            LEFT JOIN FETCH e.originalExpense
            WHERE e.user = :user
                AND e.expenseType IN ('PERSONAL', 'SHARE')
                AND FUNCTION('YEAR', e.expenseDate) = :year
                AND FUNCTION('MONTH', e.expenseDate) = :month
                AND (:category IS NULL OR e.category = :category)
                AND (:search IS NULL OR LOWER(e.title) LIKE LOWER(CONCAT('%', :search, '%')))
            """,
            countQuery = """
            SELECT count(e)
            FROM Expense e
            WHERE e.user = :user
                AND e.expenseType IN ('PERSONAL', 'SHARE')
                AND FUNCTION('YEAR', e.expenseDate) = :year
                AND FUNCTION('MONTH', e.expenseDate) = :month
                AND (:category IS NULL OR e.category = :category)
                AND (:search IS NULL OR LOWER(e.title) LIKE LOWER(CONCAT('%', :search, '%')))
            """
    )
    Page<Expense> findCombinedExpenseForUser(
            @Param("user") User user,
            @Param("year") int year,
            @Param("month") int month,
            @Param("category") ExpenseCategory category,
            @Param("search") String search,
            Pageable pageable
    );

    @Query("""
            SELECT new store.lastdance.dto.expense.BaseExpenseStats(
                SUM(e.amount),
                COUNT(e.expenseId),
                MAX(e.amount)
            )
            FROM Expense e
            WHERE e.user = :user AND e.expenseType IN ('PERSONAL', 'SHARE')
                AND FUNCTION('YEAR', e.expenseDate) = :year
                AND FUNCTION('MONTH', e.expenseDate) = :month
                AND (:category IS NULL OR e.category = :category)
                AND (:search IS NULL OR LOWER(e.title) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    BaseExpenseStats getCombinedExpenseBaseStats(
            @Param("user") User user,
            @Param("year") int year,
            @Param("month") int month,
            @Param("category") ExpenseCategory category,
            @Param("search") String search
    );

    @Query("""
            SELECT e.category as category,
                    SUM(e.amount) as totalAmount,
                    COUNT(e.expenseId) as count
            FROM Expense e
            WHERE e.user = :user AND e.expenseType IN ('PERSONAL', 'SHARE')
                AND FUNCTION('YEAR', e.expenseDate) = :year
                AND FUNCTION('MONTH', e.expenseDate) = :month
                AND (:category IS NULL OR e.category = :category)
                AND (:search IS NULL OR LOWER(e.title) LIKE LOWER(CONCAT('%', :search, '%')))
            GROUP BY e.category
            """)
    List<CategoryStatsProjection> getCombinedExpenseCategoryStats(
            @Param("user") User user,
            @Param("year") int year,
            @Param("month") int month,
            @Param("category") ExpenseCategory category,
            @Param("search") String search
    );

    @Query("""
            SELECT e
            FROM Expense e
            WHERE e.user = :user AND e.expenseType IN ('PERSONAL', 'SHARE')
                AND e.amount = :maxAmount
                AND FUNCTION('YEAR', e.expenseDate) = :year
                AND FUNCTION('MONTH', e.expenseDate) = :month
                AND (:category IS NULL OR e.category = :category)
                AND (:search IS NULL OR LOWER(e.title) LIKE LOWER(CONCAT('%', :search, '%')))
            ORDER BY e.createdAt DESC
            """)
    Optional<Expense> findTopCombinedExpenseWithMaxAmount(
            @Param("user") User user,
            @Param("year") int year,
            @Param("month") int month,
            @Param("category") ExpenseCategory category,
            @Param("search") String search,
            @Param("maxAmount") BigDecimal maxAmount
    );
}