package store.lastdance.repository.expense;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import store.lastdance.domain.expense.Expense;
import store.lastdance.domain.expense.ExpenseCategory;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    // 그룹 지출 월별 조회
    /**
     * @deprecated 페이징 기능이 추가된 {@link #findGroupExpensesByMonthWithPaging(UUID, int, int, Pageable)} 또는 {@link #findGroupExpensesBySearchAndMonthWithPaging(UUID, String, int, int, Pageable)} 등을 사용하세요.
     */
    @Deprecated
    @Query("SELECT e FROM Expense e WHERE e.groupId = :groupId " +
            "AND e.expenseType = 'GROUP' " +
            "AND YEAR(e.expenseDate) = :year AND MONTH(e.expenseDate) = :month " +
            "ORDER BY e.expenseDate DESC, e.createdAt DESC")
    List<Expense> findGroupExpensesByMonth(
            @Param("groupId") UUID groupId,
            @Param("year") int year,
            @Param("month") int month);

    /**
     * @deprecated 페이징 기능이 추가된 {@link #findPersonalExpensesForCombined(UUID, int, int, ExpenseCategory, String, Pageable)} 을 사용하세요.
     */
    @Deprecated
    @Query("SELECT e FROM Expense e WHERE e.userId = :userId " +
            "AND e.expenseType = 'PERSONAL' " +
            "AND YEAR(e.expenseDate) = :year AND MONTH(e.expenseDate) = :month " +
            "ORDER BY e.expenseDate DESC, e.createdAt DESC")
    List<Expense> findPersonalExpensesByMonth(
            @Param("userId") UUID userId,
            @Param("year") int year,
            @Param("month") int month);

    /**
     * @deprecated 페이징 기능이 추가된 {@link #findPersonalExpensesForCombined(UUID, int, int, ExpenseCategory, String, Pageable)} 을 사용하세요.
     */
    @Deprecated
    @Query("SELECT e FROM Expense e WHERE e.userId = :userId " +
            "AND e.expenseType = 'PERSONAL' " +
            "AND (:category IS NULL OR e.category = :category) " +
            "AND YEAR(e.expenseDate) = :year AND MONTH(e.expenseDate) = :month " +
            "ORDER BY e.expenseDate DESC")
    List<Expense> findPersonalExpensesByCategoryAndMonth(
            @Param("userId") UUID userId,
            @Param("category") ExpenseCategory category,
            @Param("year") int year,
            @Param("month") int month);

    /**
     * @deprecated 페이징 기능이 추가된 {@link #findPersonalExpensesForCombined(UUID, int, int, ExpenseCategory, String, Pageable)} 을 사용하세요.
     */
    @Deprecated
    @Query("SELECT e FROM Expense e WHERE e.userId = :userId " +
            "AND e.expenseType = 'PERSONAL' " +
            "AND (:searchKeyword IS NULL OR e.title LIKE %:searchKeyword%) " +
            "AND YEAR(e.expenseDate) = :year AND MONTH(e.expenseDate) = :month " +
            "ORDER BY e.expenseDate DESC")
    List<Expense> findPersonalExpensesBySearch(
            @Param("userId") UUID userId,
            @Param("searchKeyword") String searchKeyword,
            @Param("year") int year,
            @Param("month") int month);

    void deleteByOriginalExpenseId(Long expenseId);

    // 사용자의 분담 지출 조회 (ExpenseType.SHARE)
    @Query("SELECT e FROM Expense e WHERE e.userId = :userId " +
            "AND e.expenseType = 'SHARE' " +
            "AND YEAR(e.expenseDate) = :year AND MONTH(e.expenseDate) = :month " +
            "ORDER BY e.expenseDate DESC, e.createdAt DESC")
    List<Expense> findShareExpensesByUserAndMonth(
            @Param("userId") UUID userId,
            @Param("year") int year,
            @Param("month") int month
    );

    List<Expense> findByOriginalExpenseIdAndUserId(Long originalExpenseId, UUID userId);


    // 권한을 포함한 조회
    @Query("SELECT e FROM Expense e LEFT JOIN GroupMember gm ON e.groupId = gm.group.groupId " +
            "WHERE e.expenseId = :expenseId AND e.expenseType != 'SHARE' AND " +
            "(e.userId = :userId OR (e.expenseType = 'GROUP' AND gm.user.userId = :userId))")
    Optional<Expense> findByExpenseIdWithPermission(
            @Param("expenseId") Long expenseId,
            @Param("userId") UUID userId);


    // 개인 지출 월별 추이 조회
    @Query("SELECT e FROM Expense e WHERE e.userId = :userId " +
            "AND e.expenseType IN ('PERSONAL', 'SHARE') " +
            "AND e.expenseDate >= :startDate AND e.expenseDate <= :endDate " +
            "AND (:category IS NULL OR e.category = :category) " +
            "ORDER BY e.expenseDate DESC, e.createdAt DESC")
    List<Expense> findPersonalExpensesByMonthRange(
            @Param("userId") UUID userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("category") ExpenseCategory category
    );

    // 그룹 지출 월별 추이 조회
    @Query("SELECT e FROM Expense e WHERE e.groupId = :groupId " +
            "AND e.expenseType = 'GROUP' " +
            "AND e.expenseDate >= :startDate AND e.expenseDate <= :endDate " +
            "AND (:category IS NULL OR e.category = :category) " +
            "ORDER BY e.expenseDate DESC, e.createdAt DESC")
    List<Expense> findGroupExpensesByMonthRange(
            @Param("groupId") UUID groupId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("category") ExpenseCategory category
    );

    // 그룹 지출 페이징 조회
    @Query("SELECT e FROM Expense e WHERE e.groupId = :groupId " +
            "AND e.expenseType = 'GROUP' " +
            "AND YEAR(e.expenseDate) = :year AND MONTH(e.expenseDate) = :month " +
            "ORDER BY e.expenseDate DESC, e.createdAt DESC")
    Page<Expense> findGroupExpensesByMonthWithPaging(
            @Param("groupId") UUID groupId,
            @Param("year") int year,
            @Param("month") int month,
            Pageable pageable
    );

    // 그룹 분담 지출 페이징 조회 (groupId 추가)
    @Query("SELECT e FROM Expense e WHERE e.userId = :userId " +
            "AND e.groupId = :groupId " +
            "AND e.expenseType = 'SHARE' " +
            "AND YEAR(e.expenseDate) = :year AND MONTH(e.expenseDate) = :month " +
            "ORDER BY e.expenseDate DESC, e.createdAt DESC")
    Page<Expense> findShareExpensesByGroupAndMonthWithPaging(
            @Param("userId") UUID userId,
            @Param("groupId") UUID groupId,
            @Param("year") int year,
            @Param("month") int month,
            Pageable pageable
    );

    // 통합 조회용 - 개인 지출
    @Query("SELECT e FROM Expense e WHERE e.userId = :userId " +
            "AND e.expenseType = 'PERSONAL' " +
            "AND YEAR(e.expenseDate) = :year AND MONTH(e.expenseDate) = :month " +
            "AND (:category IS NULL OR e.category = :category) " +
            "AND (:search IS NULL OR e.title LIKE %:search% OR e.memo LIKE %:search%) " +
            "ORDER BY e.expenseDate DESC, e.createdAt DESC")
    Page<Expense> findPersonalExpensesForCombined(
            @Param("userId") UUID userId,
            @Param("year") int year,
            @Param("month") int month,
            @Param("category") ExpenseCategory category,
            @Param("search") String search,
            Pageable pageable
    );

    // 통합 조회용 - 분담 지출
    @Query("SELECT e FROM Expense e WHERE e.userId = :userId " +
            "AND e.expenseType = 'SHARE' " +
            "AND YEAR(e.expenseDate) = :year AND MONTH(e.expenseDate) = :month " +
            "AND (:category IS NULL OR e.category = :category) " +
            "AND (:search IS NULL OR e.title LIKE %:search% OR e.memo LIKE %:search%) " +
            "ORDER BY e.expenseDate DESC, e.createdAt DESC")
    Page<Expense> findShareExpensesForCombined(
            @Param("userId") UUID userId,
            @Param("year") int year,
            @Param("month") int month,
            @Param("category") ExpenseCategory category,
            @Param("search") String search,
            Pageable pageable
    );

    // 그룹 지출 검색 (페이징)
    @Query("SELECT e FROM Expense e WHERE e.groupId = :groupId " +
            "AND YEAR(e.expenseDate) = :year AND MONTH(e.expenseDate) = :month " +
            "AND e.expenseType = 'GROUP' " +
            "AND (:search IS NULL OR e.title LIKE %:search% OR e.memo LIKE %:search%)")
    Page<Expense> findGroupExpensesBySearchAndMonthWithPaging(
            @Param("groupId") UUID groupId,
            @Param("search") String search,
            @Param("year") int year,
            @Param("month") int month,
            Pageable pageable
    );

    // 그룹 지출 카테고리별 조회 (페이징)
    @Query("SELECT e FROM Expense e WHERE e.groupId = :groupId " +
            "AND YEAR(e.expenseDate) = :year AND MONTH(e.expenseDate) = :month " +
            "AND e.expenseType = 'GROUP' AND e.category = :category")
    Page<Expense> findGroupExpensesByCategoryAndMonthWithPaging(
            @Param("groupId") UUID groupId,
            @Param("category") ExpenseCategory category,
            @Param("year") int year,
            @Param("month") int month,
            Pageable pageable
    );
}