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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    void deleteByOriginalExpense(Expense expense);
    
    @Modifying
    void deleteByGroup(Group group);

    // 사용자의 분담 지출 조회 (ExpenseType.SHARE)
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

    // 권한을 포함한 조회
    @Query(value = "SELECT e.* FROM expense e LEFT JOIN group_member gm ON e.group_id = gm.group_id " +
                   "WHERE e.expense_id = :expenseId AND e.expense_type != 'SHARE' AND " +
                   "(e.user_id = :user OR (e.expense_type = 'GROUP' AND gm.user_id = :user))",
            nativeQuery = true)
    Optional<Expense> findByExpenseIdWithPermission(
            @Param("expenseId") Long expenseId,
            @Param("user") User user);


    // 개인 지출 월별 추이 조회
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

    // 그룹 지출 월별 추이 조회
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

    // 그룹 지출 페이징 조회
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

    // 그룹 분담 지출 페이징 조회 + 카테고리와 검색을 지원
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

    // 통합 조회용 - 개인 지출
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

    // 통합 조회용 - 분담 지출
    @Query(value = "SELECT e.*, oe.expense_id as original_expense_id, oe.amount as original_expense_amount, " +
                   "oe.description as original_expense_description, oe.expense_date as original_expense_date, " +
                   "oe.expense_type as original_expense_type, oe.category as original_expense_category, " +
                   "oe.memo as original_expense_memo, oe.created_at as original_expense_created_at, " +
                   "oe.updated_at as original_expense_updated_at, oe.user_id as original_expense_user_id, " +
                   "oe.group_id as original_expense_group_id, " +
                   "g.group_id as group_id, g.group_name as group_name, g.invite_code as group_invite_code, " +
                   "g.owner_id as group_owner_id, g.created_at as group_created_at, g.updated_at as group_updated_at " +
                   "FROM expense e " +
                   "LEFT JOIN expense oe ON e.original_expense_id = oe.expense_id " +
                   "LEFT JOIN groups g ON e.group_id = g.group_id " +
                   "WHERE e.user_id = :user " +
                   "AND e.expense_type = 'SHARE' " +
                   "AND EXTRACT(YEAR FROM e.expense_date) = :year AND EXTRACT(MONTH FROM e.expense_date) = :month " +
                   "AND (:category IS NULL OR e.category = :category) " +
                   "AND (:search IS NULL OR e.title LIKE %:search% OR e.memo LIKE %:search%) " +
                   "ORDER BY e.expense_date DESC, e.created_at DESC",
            countQuery = "SELECT COUNT(e.expense_id) FROM expense e " +
                         "LEFT JOIN expense oe ON e.original_expense_id = oe.expense_id " +
                         "LEFT JOIN groups g ON e.group_id = g.group_id " +
                         "WHERE e.user_id = :user " +
                         "AND e.expense_type = 'SHARE' " +
                         "AND EXTRACT(YEAR FROM e.expense_date) = :year AND EXTRACT(MONTH FROM e.expense_date) = :month " +
                         "AND (:category IS NULL OR e.category = :category) " +
                         "AND (:search IS NULL OR e.title LIKE %:search% OR e.memo LIKE %:search%)",
            nativeQuery = true)
    Page<Expense> findShareExpensesForCombined(
            @Param("user") User user,
            @Param("year") int year,
            @Param("month") int month,
            @Param("category") ExpenseCategory category,
            @Param("search") String search,
            Pageable pageable
    );

    // 그룹 지출 검색 (페이징)
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

    // 그룹 지출 카테고리별 조회 (페이징)
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

}