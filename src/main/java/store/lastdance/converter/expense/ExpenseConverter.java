package store.lastdance.converter.expense;

import org.springframework.stereotype.Component;
import store.lastdance.domain.expense.Expense;
import store.lastdance.domain.expense.ExpenseSplit;
import store.lastdance.domain.expense.SplitType;
import store.lastdance.domain.user.User;
import store.lastdance.dto.calender.DateRangeDTO;
import store.lastdance.dto.expense.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class ExpenseConverter {

    private static final String TYPE_PERSONAL = "PERSONAL";
    private static final String TYPE_SHARE = "SHARE";
    private static final String TYPE_GROUP = "GROUP";

    public ExpenseResponseDTO toResponseDTO(Expense expense) {
        return toResponseDTO(expense, null);
    }

    public ExpenseResponseDTO toResponseDTO(Expense expense, List<SplitDataDTO> splitData) {
        if (expense == null) {
            return null;
        }

        List<SplitDataDTO> splitSafe = (splitData == null) ? List.of() : splitData;
        return new ExpenseResponseDTO(
                expense.getExpenseId(),
                expense.getTitle(),
                expense.getAmount(),
                expense.getCategory(),
                expense.getExpenseType(),
                expense.getSplitType(),
                splitSafe,
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
                TYPE_PERSONAL,
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
                TYPE_SHARE,
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

    public GroupShareExpenseResponseDTO toGroupShareExpenseResponseDTO(Expense shareExpense, Expense originalExpense, String groupName, List<SplitDataDTO> splitData) {

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
                getReceiptImageFileId(originalExpense),
                hasReceipt(originalExpense),
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

        List<User> safeParticipants = participants != null ? participants : List.of();
        List<ExpenseSplit> safeSplits = splits != null ? splits : List.of();
        List<GroupCombinedExpenseResponseDTO.ParticipantDTO> participantDTOs =
                createParticipantDTOs(safeParticipants, safeSplits);

        return new GroupCombinedExpenseResponseDTO(
                expense.getExpenseId(),
                TYPE_GROUP,
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

        if (participants == null || participants.isEmpty()) {
            return List.of();
        }

        Map<UUID, BigDecimal> splitsMap = (splits == null || splits.isEmpty())
                ? Map.of()
                : splits.stream()
                    .filter(split -> split.getUser() != null && split.getUser().getUserId() != null)
                    .collect(Collectors.toMap(split -> split.getUser().getUserId(), ExpenseSplit::getAmount, (existing, replacement) -> replacement));

        return participants.stream()
                .map(participant -> {
                    BigDecimal shareAmount = splitsMap.getOrDefault(participant.getUserId(), BigDecimal.ZERO);
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
                endDate.atTime(LocalTime.MAX)
        );
    }

    private int calculateTotalCount(Map<String, List<ExpenseResponseDTO>> monthlyData) {
        return monthlyData.values().stream()
                .mapToInt(List::size)
                .sum();
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

    private BigDecimal determineAmount(Expense shareExpense, Expense originalExpense) {
        return originalExpense != null ? originalExpense.getAmount() : shareExpense.getAmount();
    }

    private SplitType determineSplitType(Expense shareExpense, Expense originalExpense) {
        return originalExpense != null ? originalExpense.getSplitType() : shareExpense.getSplitType();
    }
}