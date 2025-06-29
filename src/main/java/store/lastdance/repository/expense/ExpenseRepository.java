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

    // 개인 지출 월별 조회
    @Query("SELECT e FROM Expense e WHERE e.userId = :userId " +
            "AND e.groupId IS NULL " +
            "AND YEAR(e.expenseDate) = :year AND MONTH(e.expenseDate) = :month " +
            "ORDER BY e.expenseDate DESC, e.createdAt DESC")
    List<Expense> findPersonalExpensesByMonth(@Param("userId") UUID userId,
                                              @Param("year") int year,
                                              @Param("month") int month);

    // 그룹 지출 월별 조회
    @Query("SELECT e FROM Expense e WHERE e.groupId = :groupId " +
            "AND YEAR(e.expenseDate) = :year AND MONTH(e.expenseDate) = :month " +
            "ORDER BY e.expenseDate DESC, e.createdAt DESC")
    List<Expense> findGroupExpensesByMonth(@Param("groupId") UUID groupId,
                                           @Param("year") int year,
                                           @Param("month") int month);

    // 개인 지출 + 카테고리 필터
    @Query("SELECT e FROM Expense e WHERE e.userId = :userId " +
            "AND e.groupId IS NULL " +
            "AND (:category IS NULL OR e.category = :category) " +
            "AND YEAR(e.expenseDate) = :year AND MONTH(e.expenseDate) = :month " +
            "ORDER BY e.expenseDate DESC")
    List<Expense> findPersonalExpensesByCategoryAndMonth(
            @Param("userId") UUID userId,
            @Param("category") ExpenseCategory category,
            @Param("year") int year,
            @Param("month") int month
    );

    // 제목 검색 (개인 지출)
    @Query("SELECT e FROM Expense e WHERE e.userId = :userId " +
            "AND e.groupId IS NULL " +
            "AND (:searchKeyword IS NULL OR e.title LIKE %:searchKeyword%) " +
            "AND YEAR(e.expenseDate) = :year AND MONTH(e.expenseDate) = :month " +
            "ORDER BY e.expenseDate DESC")
    List<Expense> findPersonalExpensesBySearch(
            @Param("userId") UUID userId,
            @Param("searchKeyword") String searchKeyword,
            @Param("year") int year,
            @Param("month") int month
    );

    // 개인 지출 + 분담받은 지출 조회 (PERSONAL + SHARE)
    @Query("SELECT e FROM Expense e WHERE e.userId = :userId " +
            "AND (e.expenseType = 'PERSONAL' OR e.expenseType = 'SHARE') " +
            "AND YEAR(e.expenseDate) = :year AND MONTH(e.expenseDate) = :month " +
            "ORDER BY e.expenseDate DESC, e.createdAt DESC")
    List<Expense> findPersonalAndShareExpensesByMonth(
            @Param("userId") UUID userId,
            @Param("year") int year,
            @Param("month") int month);

    // 카테고리 필터 + 분담 지출 포함
    @Query("SELECT e FROM Expense e WHERE e.userId = :userId " +
            "AND (e.expenseType = 'PERSONAL' OR e.expenseType = 'SHARE') " +
            "AND (:category IS NULL OR e.category = :category) " +
            "AND YEAR(e.expenseDate) = :year AND MONTH(e.expenseDate) = :month " +
            "ORDER BY e.expenseDate DESC")
    List<Expense> findPersonalAndShareExpensesByCategoryAndMonth(
            @Param("userId") UUID userId,
            @Param("category") ExpenseCategory category,
            @Param("year") int year,
            @Param("month") int month);

    // 검색 + 분담 지출 포함
    @Query("SELECT e FROM Expense e WHERE e.userId = :userId " +
            "AND (e.expenseType = 'PERSONAL' OR e.expenseType = 'SHARE') " +
            "AND (:searchKeyword IS NULL OR e.title LIKE %:searchKeyword%) " +
            "AND YEAR(e.expenseDate) = :year AND MONTH(e.expenseDate) = :month " +
            "ORDER BY e.expenseDate DESC")
    List<Expense> findPersonalAndShareExpensesBySearch(
            @Param("userId") UUID userId,
            @Param("searchKeyword") String searchKeyword,
            @Param("year") int year,
            @Param("month") int month);

    void deleteByOriginalExpenseId(Long expenseId);
}