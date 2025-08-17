package store.lastdance.service.expense;

import org.springframework.stereotype.Component;
import store.lastdance.domain.expense.SplitType;
import store.lastdance.domain.user.User;
import store.lastdance.dto.expense.SplitDataDTO;
import store.lastdance.exception.CustomException;
import store.lastdance.exception.ErrorCode;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ExpenseSplitterImpl implements ExpenseSplitter {

    @Override
    public Map<User, BigDecimal> split(SplitType splitType, BigDecimal totalAmount, List<User> members, List<SplitDataDTO> customSplitData) {

        return switch (splitType) {
            case EQUAL -> calculateEqualSplit(totalAmount, members);
            case CUSTOM, SPECIFIC -> {
                if (customSplitData == null ||
                        customSplitData.isEmpty()) {
                    throw new CustomException
                            (ErrorCode.INVALID_SPLIT_DATA);
                }
                yield validateAndGetCustomSplit(totalAmount, customSplitData, members);
            }
        };
    }

    private Map<User, BigDecimal> calculateEqualSplit(BigDecimal totalAmount, List<User> members) {
        int memberCount = members.size();
        if (memberCount == 0) {
            return new HashMap<>();
        }

        BigDecimal baseAmount = totalAmount.divide(BigDecimal.valueOf(memberCount), 0, RoundingMode.DOWN);
        BigDecimal remainder = totalAmount.subtract(baseAmount.multiply(BigDecimal.valueOf(memberCount)));

        Map<User, BigDecimal> splitAmountMap = new HashMap<>();
        for (int i = 0; i < memberCount; i++) {
            BigDecimal finalAmount = baseAmount;
            if (i < remainder.intValue()) {
                finalAmount = finalAmount.add(BigDecimal.ONE);
            }
            splitAmountMap.put(members.get(i), finalAmount);
        }

        return splitAmountMap;
    }

    private Map<User, BigDecimal> validateAndGetCustomSplit(BigDecimal totalAmount, List<SplitDataDTO> splitData, List<User> members) {
        BigDecimal customTotal = splitData.stream()
                .map(SplitDataDTO::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalAmount.compareTo(customTotal) != 0) {
            throw new CustomException(ErrorCode.INVALID_SPLIT_AMOUNT);
        }

        Map<UUID, User> memberMap = members.stream()
                .collect(Collectors.toMap(User::getUserId, Function.identity()));

        Map<User, BigDecimal> splitAmountMap = new HashMap<>();
        for (SplitDataDTO dto : splitData) {
            User member = memberMap.get(dto.userId());
            if (member == null) {
                throw new CustomException(ErrorCode.GROUP_MEMBER_NOT_FOUND);
            }
            splitAmountMap.put(member, dto.amount());
        }

        return splitAmountMap;
    }
}
