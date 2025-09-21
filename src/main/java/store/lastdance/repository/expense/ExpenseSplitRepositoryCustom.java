package store.lastdance.repository.expense;

import store.lastdance.domain.expense.ExpenseSplit;
import store.lastdance.domain.user.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface ExpenseSplitRepositoryCustom {

    List<ExpenseSplit> findUnpaidSplitsByUserAndDate(User user,
                                                     LocalDateTime startDate,
                                                     LocalDateTime endDate);

    void deleteByGroupId(UUID groupId);
}