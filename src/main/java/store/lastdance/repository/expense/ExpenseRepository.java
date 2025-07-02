package store.lastdance.repository.expense;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import store.lastdance.domain.expense.Expense;
import store.lastdance.domain.expense.ExpenseCategory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    // 권한 체크용
    boolean existsByExpenseIdAndUserId(Long expenseId, UUID userId);

    // 지출 상세 조회 (권한 체크 포함)
    Optional<Expense> findByExpenseIdAndUserId(Long expenseId, UUID userId);

    // 그룹 지출 월별 조회
    @Query("SELECT e FROM Expense e WHERE e.groupId = :groupId " +
            "AND e.expenseType = 'GROUP' " +
            "AND YEAR(e.expenseDate) = :year AND MONTH(e.expenseDate) = :month " +
            "ORDER BY e.expenseDate DESC, e.createdAt DESC")
    List<Expense> findGroupExpensesByMonth(
            @Param("groupId") UUID groupId,
            @Param("year") int year,
            @Param("month") int month);

    // 개인 지출 조회 (PERSONAL)
    @Query("SELECT e FROM Expense e WHERE e.userId = :userId " +
            "AND e.expenseType = 'PERSONAL' " +
            "AND YEAR(e.expenseDate) = :year AND MONTH(e.expenseDate) = :month " +
            "ORDER BY e.expenseDate DESC, e.createdAt DESC")
    List<Expense> findPersonalExpensesByMonth(
            @Param("userId") UUID userId,
            @Param("year") int year,
            @Param("month") int month);

    // 개인 - 카테고리 필터
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

    // 개인 - 검색
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

}