package store.lastdance.repository.expense;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberTemplate;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import store.lastdance.domain.expense.Expense;
import store.lastdance.domain.expense.ExpenseCategory;
import store.lastdance.domain.expense.ExpenseType;
import store.lastdance.domain.group.Group;
import store.lastdance.domain.user.User;
import store.lastdance.dto.expense.BaseExpenseStats;
import store.lastdance.dto.expense.SimpleExpenseStats;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static store.lastdance.domain.expense.QExpense.expense;
import static store.lastdance.domain.group.QGroupMember.groupMember;

@Repository
@RequiredArgsConstructor
public class ExpenseRepositoryImpl implements ExpenseRepositoryCustom {
    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<Expense> findByExpenseIdWithPermission(Long expenseId, User user) {
        Expense result = queryFactory
                .selectFrom(expense)
                .leftJoin(groupMember).on(expense.group.eq(groupMember.group))
                .where(expense.expenseId.eq(expenseId),
                        expense.expenseType.ne(ExpenseType.SHARE),
                        expense.user.eq(user).or(groupMember.user.eq(user).and(expense.expenseType.eq(ExpenseType.GROUP)))
                )
                .fetchOne();
        return Optional.ofNullable(result);
    }

    @Override
    public List<Expense> findPersonalExpensesByMonthRange(User user, LocalDate startDate, LocalDate endDate, ExpenseCategory category) {
        return queryFactory
                .selectFrom(expense)
                .where(expense.user.eq(user),
                        expense.expenseType.in(ExpenseType.PERSONAL, ExpenseType.SHARE),
                        expense.expenseDate.between(startDate, endDate),
                        categoryEq(category)
                )
                .orderBy(expense.expenseDate.desc(), expense.createdAt.desc())
                .fetch();
    }

    @Override
    public Page<Expense> findPersonalExpensesForCombined(User user, int year, int month, ExpenseCategory category, String search, Pageable pageable) {
        List<Expense> content = queryFactory
                .selectFrom(expense)
                .where(
                        expense.user.eq(user),
                        expense.expenseType.eq(ExpenseType.PERSONAL),
                        yearEq(year),
                        monthEq(month),
                        categoryEq(category),
                        searchContains(search)
                )
                .orderBy(expense.expenseDate.desc(), expense.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
        Long total = queryFactory
                .select(expense.count())
                .from(expense)
                .where(
                        expense.user.eq(user),
                        expense.expenseType.eq(ExpenseType.PERSONAL),
                        yearEq(year),
                        monthEq(month),
                        categoryEq(category),
                        searchContains(search)
                )
                .fetchOne();

        long totalCount = (total != null) ? total : 0L;

        return new PageImpl<>(content, pageable, totalCount);
    }

    @Override
    public List<Expense> findPersonalAndShareExpensesByDateRange(User user, LocalDate startDate, LocalDate endDate) {
        return queryFactory
                .selectFrom(expense)
                .where(
                        expense.user.eq(user),
                        expense.expenseType.in(ExpenseType.PERSONAL, ExpenseType.SHARE),
                        expense.expenseDate.between(startDate, endDate)
                )
                .orderBy(expense.expenseDate.asc())
                .fetch();
    }

    @Override
    public List<Expense> findGroupExpensesByMonthRange(Group group, LocalDate startDate, LocalDate endDate, ExpenseCategory category) {
        return queryFactory
                .selectFrom(expense)
                .where(
                        expense.group.eq(group),
                        expense.expenseType.eq(ExpenseType.GROUP),
                        expense.expenseDate.between(startDate, endDate),
                        categoryEq(category)
                )
                .orderBy(expense.expenseDate.desc(), expense.createdAt.desc())
                .fetch();
    }

    @Override
    public Page<Expense> findGroupExpensesByMonthWithPaging(Group group, int year, int month, Pageable pageable) {
        List<Expense> content = queryFactory
                .selectFrom(expense)
                .where(
                        expense.group.eq(group),
                        expense.expenseType.eq(ExpenseType.GROUP),
                        yearEq(year),
                        monthEq(month)
                )
                .orderBy(expense.expenseDate.desc(), expense.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(expense.count())
                .from(expense)
                .where(
                        expense.group.eq(group),
                        expense.expenseType.eq(ExpenseType.GROUP),
                        yearEq(year),
                        monthEq(month)
                )
                .fetchOne();

        long totalCount = (total != null) ? total : 0L;

        return new PageImpl<>(content, pageable, totalCount);
    }

    @Override
    public BaseExpenseStats getGroupExpenseBaseStats(Group group, int year, int month, ExpenseCategory category, String search) {
        BaseExpenseStats stats = queryFactory
                .select(Projections.constructor(BaseExpenseStats.class,
                        expense.amount.sum(),
                        expense.count(),
                        expense.amount.max()
                ))
                .from(expense)
                .where(
                        expense.group.eq(group),
                        expense.expenseType.eq(ExpenseType.GROUP),
                        yearEq(year),
                        monthEq(month),
                        categoryEq(category),
                        searchContains(search)
                )
                .fetchOne();

        return stats != null ? stats : new BaseExpenseStats(null, null, null);
    }

    public List<CategoryStatsProjection> getGroupExpenseCategoryStats(Group group, int year, int month, ExpenseCategory category, String search) {
        return queryFactory
                .select(Projections.bean(CategoryStatsProjection.class,
                        expense.category,
                        expense.amount.sum().as("totalAmount"),
                        expense.count().as("count")
                ))
                .from(expense)
                .where(
                        expense.group.eq(group),
                        expense.expenseType.eq(ExpenseType.GROUP),
                        yearEq(year),
                        monthEq(month),
                        categoryEq(category),
                        searchContains(search)
                )
                .groupBy(expense.category)
                .fetch();
    }

    @Override
    public Optional<Expense> findTopGroupExpenseWithMaxAmount(Group group, int year, int month, ExpenseCategory category, String search, BigDecimal maxAmount) {
        Expense result = queryFactory
                .selectFrom(expense)
                .where(
                        expense.group.eq(group),
                        expense.expenseType.eq(ExpenseType.GROUP),
                        expense.amount.eq(maxAmount),
                        yearEq(year),
                        monthEq(month),
                        categoryEq(category),
                        searchContains(search)
                )
                .orderBy(expense.createdAt.desc())
                .fetchOne();

        return Optional.ofNullable(result);
    }

    @Override
    public List<Expense> findShareExpensesByUserAndMonth(User user, int year, int month) {
        return queryFactory
                .selectFrom(expense)
                .leftJoin(expense.originalExpense).fetchJoin()
                .leftJoin(expense.group).fetchJoin()
                .where(
                        expense.user.eq(user),
                        expense.expenseType.eq(ExpenseType.SHARE),
                        yearEq(year),
                        monthEq(month)
                )
                .orderBy(expense.expenseDate.desc(), expense.createdAt.desc())
                .fetch();
    }

    @Override
    public Page<Expense> findShareExpensesByGroupAndMonthWithPagingFiltered(User user, Group group, int year, int month, ExpenseCategory category, String search, Pageable pageable) {
        List<Expense> content = queryFactory
                .selectFrom(expense)
                .where(
                        expense.user.eq(user),
                        expense.group.eq(group),
                        expense.expenseType.eq(ExpenseType.SHARE),
                        yearEq(year),
                        monthEq(month),
                        categoryEq(category),
                        searchContains(search)
                )
                .orderBy(expense.expenseDate.desc(), expense.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(expense.count())
                .from(expense)
                .where(
                        expense.user.eq(user),
                        expense.group.eq(group),
                        expense.expenseType.eq(ExpenseType.SHARE),
                        yearEq(year),
                        monthEq(month),
                        categoryEq(category),
                        searchContains(search)
                )
                .fetchOne();
        long totalCount = (total != null) ? total : 0L;

        return new PageImpl<>(content, pageable, totalCount);
    }

    @Override
    public Page<Expense> findShareExpensesForCombined(User user, int year, int month, ExpenseCategory category, String search, Pageable pageable) {
        List<Expense> content = queryFactory
                .selectFrom(expense)
                .leftJoin(expense.originalExpense).fetchJoin()
                .leftJoin(expense.group).fetchJoin()
                .where(
                        expense.user.eq(user),
                        expense.expenseType.eq(ExpenseType.SHARE),
                        yearEq(year),
                        monthEq(month),
                        categoryEq(category),
                        searchContains(search)
                )
                .orderBy(expense.expenseDate.desc(), expense.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(expense.count())
                .from(expense)
                .where(
                        expense.user.eq(user),
                        expense.expenseType.eq(ExpenseType.SHARE),
                        yearEq(year),
                        monthEq(month),
                        categoryEq(category),
                        searchContains(search)
                )
                .fetchOne();
        long totalCount = (total != null) ? total : 0L;

        return new PageImpl<>(content, pageable, totalCount);
    }

    @Override
    public SimpleExpenseStats getShareExpenseBaseStats(User user, Group group, int year, int month, ExpenseCategory category, String search) {
        SimpleExpenseStats stats = queryFactory
                .select(Projections.constructor(SimpleExpenseStats.class,
                        expense.originalExpense.amount.sum(),
                        expense.amount.sum(),
                        expense.count(),
                        expense.amount.max()
                ))
                .from(expense)
                .where(
                        expense.user.eq(user),
                        expense.group.eq(group),
                        expense.expenseType.eq(ExpenseType.SHARE),
                        yearEq(year),
                        monthEq(month),
                        categoryEq(category),
                        searchContains(search)
                )
                .fetchOne();

        return (stats != null) ? stats : new SimpleExpenseStats(null, null, null, null);
    }

    @Override
    public List<CategoryStatsProjection> getShareExpenseCategoryStats(User user, Group group, int year, int month, ExpenseCategory category, String search) {
        return queryFactory
                .select(Projections.bean(CategoryStatsProjection.class,
                        expense.originalExpense.category.as("category"),
                        expense.amount.sum().as("totalAmount"),
                        expense.count().as("count")
                ))
                .from(expense)
                .where(
                        expense.user.eq(user),
                        expense.group.eq(group),
                        expense.expenseType.eq(ExpenseType.SHARE),
                        yearEq(year),
                        monthEq(month),
                        categoryEq(category),
                        searchContains(search)
                )
                .groupBy(expense.originalExpense.category)
                .fetch();
    }

    @Override
    public Optional<Expense> findTopShareExpenseWithMaxAmount(User user, Group group, int year, int month, ExpenseCategory category, String search, BigDecimal maxAmount) {
        Expense result = queryFactory
                .selectFrom(expense)
                .where(
                        expense.user.eq(user),
                        expense.group.eq(group),
                        expense.expenseType.eq(ExpenseType.SHARE),
                        expense.amount.eq(maxAmount),
                        yearEq(year),
                        monthEq(month),
                        categoryEq(category),
                        searchContains(search)
                )
                .orderBy(expense.createdAt.desc())
                .fetchOne();

        return Optional.ofNullable(result);
    }

    @Override
    public Page<Expense> findCombinedExpenseForUser(User user, int year, int month, ExpenseCategory category, String search, Pageable pageable) {
        List<Expense> content = queryFactory
                .selectFrom(expense)
                .leftJoin(expense.originalExpense).fetchJoin()
                .where(
                        expense.user.eq(user),
                        expense.expenseType.in(ExpenseType.PERSONAL, ExpenseType.SHARE),
                        yearEq(year),
                        monthEq(month),
                        categoryEq(category),
                        searchContains(search)
                )
                .orderBy(expense.expenseDate.desc(), expense.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(expense.count())
                .from(expense)
                .where(
                        expense.user.eq(user),
                        expense.expenseType.in(ExpenseType.PERSONAL, ExpenseType.SHARE),
                        yearEq(year),
                        monthEq(month),
                        categoryEq(category),
                        searchContains(search)
                )
                .fetchOne();

        long totalCount = (total != null) ? total : 0L;

        return new PageImpl<>(content, pageable, totalCount);
    }

    @Override
    public Page<Expense> findGroupExpensesBySearchAndMonthWithPaging(Group group, String search, int year, int month, Pageable pageable) {
        List<Expense> content = queryFactory
                .selectFrom(expense)
                .where(
                        expense.group.eq(group),
                        expense.expenseType.eq(ExpenseType.GROUP),
                        yearEq(year),
                        monthEq(month),
                        searchContains(search)
                )
                .orderBy(expense.expenseDate.desc(), expense.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(expense.count())
                .from(expense)
                .where(
                        expense.group.eq(group),
                        expense.expenseType.eq(ExpenseType.GROUP),
                        yearEq(year),
                        monthEq(month),
                        searchContains(search)
                )
                .fetchOne();
        long totalCount = (total != null) ? total : 0L;

        return new PageImpl<>(content, pageable, totalCount);
    }

    @Override
    public Page<Expense> findGroupExpensesByCategoryAndMonthWithPaging(Group group, ExpenseCategory category, int year, int month, Pageable pageable) {
        List<Expense> content = queryFactory
                .selectFrom(expense)
                .where(
                        expense.group.eq(group),
                        expense.expenseType.eq(ExpenseType.GROUP),
                        yearEq(year),
                        monthEq(month),
                        categoryEq(category)
                )
                .orderBy(expense.expenseDate.desc(), expense.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(expense.count())
                .from(expense)
                .where(
                        expense.group.eq(group),
                        expense.expenseType.eq(ExpenseType.GROUP),
                        yearEq(year),
                        monthEq(month),
                        categoryEq(category)
                )
                .fetchOne();
        long totalCount = (total != null) ? total : 0L;
        return new PageImpl<>(content, pageable, totalCount);
    }

    @Override
    public BaseExpenseStats getCombinedExpenseBaseStats(User user, int year, int month, ExpenseCategory category, String search) {
        BaseExpenseStats stats = queryFactory
                .select(Projections.constructor(BaseExpenseStats.class,
                        expense.amount.sum(),
                        expense.count(),
                        expense.amount.max()
                ))
                .from(expense)
                .where(
                        expense.user.eq(user),
                        expense.expenseType.in(ExpenseType.PERSONAL, ExpenseType.SHARE),
                        yearEq(year),
                        monthEq(month),
                        categoryEq(category),
                        searchContains(search)
                )
                .fetchOne();

        return (stats != null) ? stats : new BaseExpenseStats(null, null, null);
    }

    @Override
    public List<CategoryStatsProjection> getCombinedExpenseCategoryStats(User user, int year, int month, ExpenseCategory category, String search) {
        return queryFactory
                .select(Projections.bean(CategoryStatsProjection.class,
                        expense.category,
                        expense.amount.sum().as("totalAmount"),
                        expense.count().as("count")
                ))
                .from(expense)
                .where(
                        expense.user.eq(user),
                        expense.expenseType.in(ExpenseType.PERSONAL, ExpenseType.SHARE),
                        yearEq(year),
                        monthEq(month),
                        categoryEq(category),
                        searchContains(search)
                )
                .groupBy(expense.category)
                .fetch();
    }

    @Override
    public Optional<Expense> findTopCombinedExpenseWithMaxAmount(User user, int year, int month, ExpenseCategory category, String search, BigDecimal maxAmount) {
        Expense result = queryFactory
                .selectFrom(expense)
                .where(
                        expense.user.eq(user),
                        expense.expenseType.in(ExpenseType.PERSONAL, ExpenseType.SHARE),
                        expense.amount.eq(maxAmount),
                        yearEq(year),
                        monthEq(month),
                        categoryEq(category),
                        searchContains(search)
                )
                .orderBy(expense.createdAt.desc())
                .fetchOne();

        return Optional.ofNullable(result);
    }

    private BooleanExpression yearEq(int year) {
        NumberTemplate<Integer> yearTemplate = Expressions.numberTemplate(Integer.class, "function('YEAR', {0})", expense.expenseDate);
        return yearTemplate.eq(year);
    }

    private BooleanExpression monthEq(int month) {
        NumberTemplate<Integer> monthTemplate = Expressions.numberTemplate(Integer.class, "function('MONTH', {0})", expense.expenseDate);
        return monthTemplate.eq(month);
    }

    private BooleanExpression searchContains(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        return expense.title.containsIgnoreCase(search).or(expense.memo.containsIgnoreCase(search));
    }

    private BooleanExpression categoryEq(ExpenseCategory category) {
        return category != null ? expense.category.eq(category) : null;
    }
}
