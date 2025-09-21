package store.lastdance.repository.expense;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import store.lastdance.domain.expense.ExpenseSplit;
import store.lastdance.domain.user.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static store.lastdance.domain.expense.QExpenseSplit.expenseSplit;

@Repository
@RequiredArgsConstructor
public class ExpenseSplitRepositoryImpl implements ExpenseSplitRepositoryCustom {
    private final JPAQueryFactory queryFactory;

    @Override
    public List<ExpenseSplit> findUnpaidSplitsByUserAndDate(User user, LocalDateTime startDate, LocalDateTime endDate) {
        return queryFactory
                .selectFrom(expenseSplit)
                .where(
                        expenseSplit.user.eq(user),
                        expenseSplit.paid.eq(false),
                        expenseSplit.createdAt.between(startDate, endDate)
                )
                .orderBy(expenseSplit.createdAt.asc())
                .fetch();
    }

    @Override
    public void deleteByGroupId(UUID groupId) {
        queryFactory
                .delete(expenseSplit)
                .where(
                        expenseSplit.expense.group.groupId.eq(groupId)
                )
                .execute();
    }
}
