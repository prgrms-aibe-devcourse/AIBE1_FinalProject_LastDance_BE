package store.lastdance.converter;

import org.springframework.stereotype.Component;
import store.lastdance.domain.expense.Expense;
import store.lastdance.domain.expense.ExpenseSplit;
import store.lastdance.domain.expense.SplitType;
import store.lastdance.domain.user.User;
import store.lastdance.dto.calender.DateRangeDTO;
import store.lastdance.dto.expense.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class ExpenseConverter {

    public ExpenseResponseDTO toResponseDTO(Expense expense) {
        return toResponseDTO(expense, null);
    }

    public ExpenseResponseDTO toResponseDTO(Expense expense, List<SplitDataDTO> splitData) {
        if (expense == null) {
            return null;
        }

        return new ExpenseResponseDTO(
                expense.getExpenseId(),
                expense.getTitle(),
                expense.getAmount(),
                expense.getCategory(),
                expense.getExpenseType(),
                expense.getSplitType(),
                splitData,
                expense.getExpenseDate(),
                expense.getMemo(),
                getGroupId(expense),
                getUserId(expense),
                expense.getCreatedAt(),
                expense.getUpdatedAt(),
                getReceiptImageFileId(expense),
                hasReceipt(expense)
        );
    }

    public CombinedExpenseResponseDTO toCombinedResponseDTO(Expense expense) {
        if (expense == null) {
            return null;
        }

        return new CombinedExpenseResponseDTO(
                expense.getExpenseId(),
                null,
                "PERSONAL",
                expense.getTitle(),
                expense.getAmount(),
                expense.getAmount(),
                expense.getCategory(),
                expense.getExpenseDate().atStartOfDay(),
                expense.getMemo(),
                hasReceipt(expense),
                null,
                null
        );
    }

    public CombinedExpenseResponseDTO toCombinedResponseDTO(Expense shareExpense, Expense originalExpense, String groupName) {

        if (shareExpense == null) {
            return null;
        }

        return new CombinedExpenseResponseDTO(
                shareExpense.getExpenseId(),
                originalExpense != null ? originalExpense.getExpenseId() : null,
                "SHARE",
                shareExpense.getTitle(),
                originalExpense != null ? originalExpense.getAmount() : shareExpense.getAmount(),
                shareExpense.getAmount(),
                shareExpense.getCategory(),
                shareExpense.getExpenseDate().atStartOfDay(),
                shareExpense.getMemo(),
                hasReceipt(originalExpense),
                getGroupId(shareExpense),
                groupName
        );
    }

    public GroupShareExpenseResponseDTO toGroupShareExpenseResponseDTO(Expense shareExpense, Expense originalExpense, String groupName,List<SplitDataDTO> splitData) {

        if (shareExpense == null) {
            return null;
        }

        return new GroupShareExpenseResponseDTO(
                shareExpense.getExpenseId(),
                shareExpense.getTitle(),
                determineAmount(shareExpense, originalExpense),
                shareExpense.getAmount(),
                shareExpense.getCategory(),
                shareExpense.getExpenseDate(),
                shareExpense.getMemo(),
                getGroupId(shareExpense),
                groupName,
                determineSplitType(shareExpense, originalExpense),
                splitData,
                getOriginalReceiptImageFileId(originalExpense),
                hasOriginalReceipt(originalExpense),
                originalExpense != null ? originalExpense.getExpenseId() : null
        );
    }

    public GroupCombinedExpenseResponseDTO toGroupCombinedResponseDTO(
            Expense expense,
            BigDecimal myShareAmount,
            String groupName,
            User creator,
            List<ExpenseSplit> splits,
            List<User> participants) {

        if (expense == null) {
            return null;
        }

        List<GroupCombinedExpenseResponseDTO.ParticipantDTO> participantDTOs =
                createParticipantDTOs(participants, splits);

        return new GroupCombinedExpenseResponseDTO(
                expense.getExpenseId(),
                "GROUP",
                expense.getTitle(),
                expense.getAmount(),
                myShareAmount,
                expense.getCategory(),
                expense.getExpenseDate(),
                expense.getMemo(),
                hasReceipt(expense),
                getGroupId(expense),
                groupName,
                creator != null ? creator.getUserId() : null,
                creator != null ? creator.getNickname() : null,
                expense.getSplitType(),
                participantDTOs
        );
    }

    private List<GroupCombinedExpenseResponseDTO.ParticipantDTO> createParticipantDTOs(
            List<User> participants, List<ExpenseSplit> splits) {

        return participants.stream()
                .map(participant -> {
                    BigDecimal shareAmount = findShareAmount(participant.getUserId(), splits);
                    return new GroupCombinedExpenseResponseDTO.ParticipantDTO(
                            participant.getUserId(),
                            participant.getNickname(),
                            shareAmount
                    );
                })
                .toList();
    }

    public MonthlyExpenseTrendResponseDTO toMonthlyTrendResponseDTO(
            Map<String, List<ExpenseResponseDTO>> monthlyData,
            LocalDate startDate,
            LocalDate endDate) {

        DateRangeDTO dateRangeDTO = createDateRangeDTO(startDate, endDate);
        int totalCount = calculateTotalCount(monthlyData);

        return new MonthlyExpenseTrendResponseDTO(monthlyData, totalCount, dateRangeDTO);
    }

    private DateRangeDTO createDateRangeDTO(LocalDate startDate, LocalDate endDate) {
        return new DateRangeDTO(
                startDate.atStartOfDay(),
                endDate.atTime(23, 59, 59)
        );
    }

    private int calculateTotalCount(Map<String, List<ExpenseResponseDTO>> monthlyData) {
        return monthlyData.values().stream()
                .mapToInt(List::size)
                .sum();
    }

    private BigDecimal findShareAmount(UUID userId, List<ExpenseSplit> splits) {
        return splits.stream()
                .filter(split -> split.getUser().getUserId().equals(userId))
                .map(ExpenseSplit::getAmount)
                .findFirst()
                .orElse(BigDecimal.ZERO);
    }

    private UUID getGroupId(Expense expense) {
        return expense != null && expense.getGroup() != null ? expense.getGroup().getGroupId() : null;
    }

    private UUID getUserId(Expense expense) {
        return expense != null && expense.getUser() != null ? expense.getUser().getUserId() : null;
    }

    private Boolean hasReceipt(Expense expense) {
        return expense != null && expense.getReceiptImageFile() != null;
    }

    private UUID getReceiptImageFileId(Expense expense) {
        return hasReceipt(expense) ? expense.getReceiptImageFile().getFileId() : null;
    }

    private Boolean hasOriginalReceipt(Expense originalExpense) {
        return originalExpense != null && originalExpense.getReceiptImageFile() != null;
    }

    private UUID getOriginalReceiptImageFileId(Expense originalExpense) {
        return hasOriginalReceipt(originalExpense) ? originalExpense.getReceiptImageFile().getFileId() : null;
    }

    private BigDecimal determineAmount(Expense shareExpense, Expense originalExpense) {
        return originalExpense != null ? originalExpense.getAmount() : shareExpense.getAmount();
    }

    private SplitType determineSplitType(Expense shareExpense, Expense originalExpense) {
        return originalExpense != null ? originalExpense.getSplitType() : shareExpense.getSplitType();
    }
}