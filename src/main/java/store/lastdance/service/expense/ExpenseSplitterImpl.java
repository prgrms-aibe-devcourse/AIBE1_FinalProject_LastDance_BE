package store.lastdance.service.expense;

import org.springframework.stereotype.Component;
import store.lastdance.domain.expense.SplitType;
import store.lastdance.domain.user.User;
import store.lastdance.dto.expense.SplitDataDTO;
import store.lastdance.exception.CustomException;
import store.lastdance.exception.ErrorCode;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class ExpenseSplitterImpl implements ExpenseSplitter {

    @Override
    public Map<UUID, BigDecimal> split(SplitType splitType, BigDecimal totalAmount, List<User> members, List<SplitDataDTO> customSplitData) {

        if (totalAmount == null || totalAmount.signum() < 0) {
            throw new CustomException(ErrorCode.INVALID_SPLIT_AMOUNT);
        }
        if (members == null) {
            members = Collections.emptyList();
        }
        return switch (splitType) {
            case EQUAL -> calculateEqualSplit(totalAmount, members);
            case CUSTOM, SPECIFIC -> {
                if (customSplitData == null ||
                        customSplitData.isEmpty()) {
                    throw new CustomException(ErrorCode.INVALID_SPLIT_DATA);
                }
                if (totalAmount.scale() > 0) {
                    throw new CustomException(ErrorCode.INVALID_SPLIT_AMOUNT);
                }
                yield validateAndGetCustomSplit(totalAmount, customSplitData, members);
            }
        };
    }

    private Map<UUID, BigDecimal> calculateEqualSplit(BigDecimal totalAmount, List<User> members) {
        int memberCount = members.size();
        if (memberCount == 0) {
            return new HashMap<>();
        }

        if (totalAmount.scale() > 0) {
            throw new CustomException(ErrorCode.INVALID_SPLIT_AMOUNT);
        }

        BigDecimal baseAmount = totalAmount.divide(BigDecimal.valueOf(memberCount), 0, RoundingMode.DOWN);
        BigDecimal remainder = totalAmount.subtract(baseAmount.multiply(BigDecimal.valueOf(memberCount)));

        var ordered = new ArrayList<>(members);
        ordered.sort(Comparator.comparing(User::getUserId));
        Map<UUID, BigDecimal> splitAmountMap = new LinkedHashMap<>();
        for (int i = 0; i < memberCount; i++) {
            BigDecimal finalAmount = baseAmount;
            if (i < remainder.intValue()) {
                finalAmount = finalAmount.add(BigDecimal.ONE);
            }
            splitAmountMap.put(ordered.get(i).getUserId(), finalAmount);
        }

        return splitAmountMap;
    }

    private Map<UUID, BigDecimal> validateAndGetCustomSplit(BigDecimal totalAmount, List<SplitDataDTO> splitData, List<User> members) {

        var userIdSeen = new HashSet<UUID>();
        BigDecimal customTotal = BigDecimal.ZERO;
        for (SplitDataDTO dto : splitData) {
            if (dto.amount() == null || dto.amount().signum() < 0) {
                throw new CustomException(ErrorCode.INVALID_SPLIT_AMOUNT);
            }
            if (totalAmount.scale() == 0 && dto.amount().scale() > 0) {
                throw new CustomException(ErrorCode.INVALID_SPLIT_AMOUNT);
            }
            if (!userIdSeen.add(dto.userId())) {
                throw new CustomException(ErrorCode.INVALID_SPLIT_DATA);
            }
            customTotal = customTotal.add(dto.amount());
        }
        if (customTotal.compareTo(totalAmount) != 0) {
            throw new CustomException(ErrorCode.INVALID_SPLIT_AMOUNT);
        }

        Set<UUID> memberIds = members.stream()
                .map(User::getUserId)
                .collect(Collectors.toSet());

        for (SplitDataDTO dto : splitData) {
            if (!memberIds.contains(dto.userId())) {
                throw new CustomException(ErrorCode.GROUP_MEMBER_NOT_FOUND);
            }
        }

        return splitData.stream()
                .collect(Collectors.toMap(
                        SplitDataDTO::userId,
                        SplitDataDTO::amount
                , (a, b) -> a, LinkedHashMap::new));
    }
}
