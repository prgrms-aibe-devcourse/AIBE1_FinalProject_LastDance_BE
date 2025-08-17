package store.lastdance.service.expense;

import store.lastdance.domain.expense.SplitType;
import store.lastdance.domain.user.User;
import store.lastdance.dto.expense.SplitDataDTO;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface ExpenseSplitter {
    Map<User, BigDecimal> split(SplitType splitType,
                                BigDecimal totalAmount,
                                List<User> members,
                                List<SplitDataDTO> customSplitData);
}
