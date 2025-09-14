package store.lastdance.service.expense;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import store.lastdance.converter.expense.ExpenseConverter;
import store.lastdance.domain.common.ImageFile;
import store.lastdance.domain.expense.Expense;
import store.lastdance.domain.expense.ExpenseCategory;
import store.lastdance.domain.expense.ExpenseSplit;
import store.lastdance.domain.expense.ExpenseType;
import store.lastdance.domain.group.Group;
import store.lastdance.domain.user.User;
import store.lastdance.dto.expense.*;
import store.lastdance.dto.response.PageWithSummaryResponse;
import store.lastdance.exception.CustomException;
import store.lastdance.exception.ErrorCode;
import store.lastdance.repository.expense.CategoryStatsProjection;
import store.lastdance.repository.expense.ExpenseRepository;
import store.lastdance.repository.expense.ExpenseSplitRepository;
import store.lastdance.repository.group.GroupMemberRepository;
import store.lastdance.repository.group.GroupRepository;
import store.lastdance.repository.user.UserRepository;
import store.lastdance.service.image.ImageService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ExpenseV2QueryServiceImpl implements ExpenseV2QueryService {
    private final ExpenseRepository expenseRepository;
    private final ExpenseSplitRepository expenseSplitRepository;
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;
    private final ImageService imageService;
    private final ExpenseConverter expenseConverter;

    private User findUserById(UUID userId) {
        return userRepository.findById(userId).orElseThrow(
                () -> new CustomException(ErrorCode.USER_NOT_FOUND)
        );
    }

    private Group findGroupById(UUID groupId) {
        return groupRepository.findById(groupId).orElseThrow(
                () -> new CustomException(ErrorCode.GROUP_NOT_FOUND)
        );
    }

    @Override
    public ExpenseResponseDTO getExpenseById(UUID userId, Long expenseId) {
        User user = findUserById(userId);

        Expense expense = expenseRepository.findByExpenseIdWithPermission(expenseId, user).orElseThrow(
                () -> new CustomException(ErrorCode.EXPENSE_NOT_FOUND)
        );

        List<SplitDataDTO> splitData = (expense.getExpenseType() == ExpenseType.GROUP)
                ? getSplitData(expense)
                : Collections.emptyList();
        return expenseConverter.toResponseDTO(expense, splitData);
    }

    private List<SplitDataDTO> getSplitData(Expense expense) {
        return expenseSplitRepository.findByExpense(expense)
                .stream()
                .map(split -> new SplitDataDTO(split.getUser().getUserId(), split.getAmount()))
                .toList();
    }

    @Override
    public List<GroupShareExpenseResponseDTO> getGroupShareExpenses(UUID userId, ExpenseSearchDTO searchDTO) {
        log.info("=== getGroupShareExpenses 호출 ===");
        log.info("userId: {}, year: {}, month: {}", userId, searchDTO.year(), searchDTO.month());

        User user = findUserById(userId);

        List<Expense> shareExpenses = expenseRepository.findShareExpensesByUserAndMonth(user, searchDTO.year(), searchDTO.month());
        log.info("조회된 SHARE 지출 개수: {}", shareExpenses.size());

        return convertShareExpensesToDTOs(shareExpenses);
    }

    @Override
    public String getReceiptImageUrl(Long expenseId, UUID userId) {

        User user = findUserById(userId);
        Expense expense = expenseRepository.findByExpenseIdWithPermission(expenseId, user).orElseThrow(
                () -> new CustomException(ErrorCode.EXPENSE_NOT_FOUND)
        );

        ImageFile receiptImageFile = expense.getReceiptImageFile();
        if (receiptImageFile == null) {
            return null;
        }

        return imageService.generatePresignedUrl(receiptImageFile.getFileId());
    }

    @Override
    public MonthlyExpenseTrendResponseDTO getPersonalExpenseTrend(UUID userId, ExpenseSearchDTO searchDTO) {
        log.info("개인 지출 추이 조회: userId={}, year={}, month={}, months={}, category={}", userId, searchDTO.year(), searchDTO.month(), searchDTO.months(), searchDTO.category());

        User user = findUserById(userId);

        DateRange dateRange = calculateDateRange(searchDTO.year(), searchDTO.month(), searchDTO.months());

        ExpenseCategory categoryEnum = parseCategory(searchDTO.category());
        List<Expense> expenses = expenseRepository.findPersonalExpensesByMonthRange(user, dateRange.startDate(), dateRange.endDate(), categoryEnum);

        return createTrendResponse(expenses, dateRange);
    }

    @Override
    public MonthlyExpenseTrendResponseDTO getGroupExpenseTrend(UUID userId, UUID groupId, ExpenseSearchDTO searchDTO) {
        User user = findUserById(userId);
        Group group = findGroupById(groupId);

        boolean isMember = groupMemberRepository.existsByGroupAndUser(group, user);
        if (!isMember) {
            throw new CustomException(ErrorCode.GROUP_MEMBER_NOT_FOUND);
        }

        log.info("그룹 지출 추이 조회: groupId={}, year={}, month={}, months={}, category={}",
                groupId, searchDTO.year(), searchDTO.month(), searchDTO.months(), searchDTO.category());

        DateRange dateRange = calculateDateRange(searchDTO.year(), searchDTO.month(), searchDTO.months());

        ExpenseCategory categoryEnum = parseCategory(searchDTO.category());
        List<Expense> expenses = expenseRepository.findGroupExpensesByMonthRange(group, dateRange.startDate(), dateRange.endDate(), categoryEnum);

        return createTrendResponse(expenses, dateRange);
    }

    private DateRange calculateDateRange(int year, int month, int months) {
        if (months < 1) {
            throw new CustomException(ErrorCode.INVALID_MONTH_REQUEST);
        }
        if (month < 1 || month > 12) {
            throw new CustomException(ErrorCode.INVALID_MONTH_REQUEST);
        }
        LocalDate endDate = LocalDate.of(year, month, 1).with(TemporalAdjusters.lastDayOfMonth());
        LocalDate startDate = endDate.minusMonths(months - 1).with(TemporalAdjusters.firstDayOfMonth());

        return new DateRange(startDate, endDate);
    }

    private MonthlyExpenseTrendResponseDTO createTrendResponse(List<Expense> expenses, DateRange dateRange) {
        Map<String, List<ExpenseResponseDTO>> monthlyData = createMonthlyGrouping(expenses, dateRange);
        return expenseConverter.toMonthlyTrendResponseDTO(monthlyData, dateRange.startDate(), dateRange.endDate());
    }

    private Map<String, List<ExpenseResponseDTO>> createMonthlyGrouping(List<Expense> expenses, DateRange dateRange) {
        List<Expense> groupExpenses = expenses.stream()
                .filter(e -> e.getExpenseType() == ExpenseType.GROUP)
                .toList();

        Map<Long, List<SplitDataDTO>> splitsByExpenseId = new HashMap<>();
        if (!groupExpenses.isEmpty()) {
            List<ExpenseSplit> allSplits = expenseSplitRepository.findByExpenseIn(groupExpenses);
            splitsByExpenseId = allSplits.stream()
                    .collect(Collectors.groupingBy(
                            split -> split.getExpense().getExpenseId(),
                            Collectors.mapping(
                                    split -> new SplitDataDTO(split.getUser().getUserId(), split.getAmount()),
                                    Collectors.toList()
                            )
                    ));
        }

        final Map<Long, List<SplitDataDTO>> finalSplitsMap = splitsByExpenseId;
        Map<String, List<ExpenseResponseDTO>> monthlyData = expenses.stream()
                .collect(Collectors.groupingBy(
                        expense -> expense.getExpenseDate().format(DateTimeFormatter.ofPattern("yyyy-MM")),
                        LinkedHashMap::new,
                        Collectors.mapping(
                                expense -> expenseConverter.toResponseDTO(
                                        expense,
                                        finalSplitsMap.getOrDefault(expense.getExpenseId(), Collections.emptyList())),
                                Collectors.toList()
                        )
                ));

        fillEmptyMonths(monthlyData, dateRange.startDate(), dateRange.endDate());
        return sortByMonthKey(monthlyData);
    }

    private void fillEmptyMonths(Map<String, List<ExpenseResponseDTO>> monthlyData, LocalDate startDate, LocalDate endDate) {
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            String monthKey = current.format(DateTimeFormatter.ofPattern("yyyy-MM"));
            monthlyData.putIfAbsent(monthKey, new ArrayList<>());
            current = current.plusMonths(1);
        }
    }

    private Map<String, List<ExpenseResponseDTO>> sortByMonthKey(Map<String, List<ExpenseResponseDTO>> monthlyData) {
        return monthlyData.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(LinkedHashMap::new, (m, e) -> m.put(e.getKey(), e.getValue()),
                        LinkedHashMap::putAll);
    }

    private record DateRange(LocalDate startDate, LocalDate endDate) {
    }

    @Override
    public PageWithSummaryResponse<GroupShareExpenseResponseDTO> getGroupShareExpensesWithPaging(
            UUID userId, UUID groupId, ExpenseSearchDTO searchDTO, Pageable pageable
    ) {
        User user = findUserById(userId);
        Group group = findGroupById(groupId);
        validateGroupMembership(user, group);

        ExpenseCategory categoryEnum = parseCategory(searchDTO.category());
        Page<Expense> shareExpensesPage = expenseRepository.findShareExpensesByGroupAndMonthWithPagingFiltered(
                user, group, searchDTO.year(), searchDTO.month(),
                categoryEnum, searchDTO.search(), pageable
        );

        List<GroupShareExpenseResponseDTO> pageContent = convertShareExpensesToDTOs(shareExpensesPage.getContent());
        Page<GroupShareExpenseResponseDTO> shareExpenseResponsePage = new PageImpl<>(pageContent, pageable, shareExpensesPage.getTotalElements());

        if (!shareExpenseResponsePage.hasContent()) {
            return PageWithSummaryResponse.of(shareExpenseResponsePage, ExpenseSummary.empty());
        }

        SimpleExpenseStats baseStats = expenseRepository.getShareExpenseBaseStats(
                user, group, searchDTO.year(), searchDTO.month(), categoryEnum, searchDTO.search()
        );
        List<CategoryStatsProjection> categoryStatsProjections = expenseRepository.getShareExpenseCategoryStats(
                user, group, searchDTO.year(), searchDTO.month(), categoryEnum, searchDTO.search()
        );

        Map<String, CategoryStats> categoryStatsMap = categoryStatsProjections.stream()
                .collect(Collectors.toMap(
                        p -> p.getCategory().name(),
                        p -> new CategoryStats(
                                p.getTotalAmount(), p.getCount(), BigDecimal.ZERO
                        )
                ));

        Long maxExpenseId = null;
        String maxExpenseTitle = null;
        if (baseStats.maxShareAmount() != null && baseStats.maxShareAmount().compareTo(BigDecimal.ZERO) > 0) {
            Optional<Expense> maxExpenseOpt = expenseRepository.findTopShareExpenseWithMaxAmount(
                    user, group, searchDTO.year(), searchDTO.month(), categoryEnum, searchDTO.search(), baseStats.maxShareAmount()
            );
            if (maxExpenseOpt.isPresent()) {
                Expense maxShareExpense = maxExpenseOpt.get();
                maxExpenseId = maxShareExpense.getExpenseId();
                maxExpenseTitle = maxShareExpense.getTitle();
            }
        }

        BigDecimal averageAmount = BigDecimal.ZERO;
        if (baseStats.totalCount() > 0) {
            averageAmount = baseStats.totalShareAmount().divide(BigDecimal.valueOf(baseStats.totalCount()), 2, RoundingMode.HALF_UP);
        }

        ExpenseSummary summary = new ExpenseSummary(
                baseStats.totalOriginalAmount(),
                averageAmount,
                baseStats.maxShareAmount(),
                baseStats.totalCount(),
                baseStats.totalShareAmount(),
                baseStats.totalCount(),
                categoryStatsMap,
                maxExpenseId,
                maxExpenseTitle
        );

        return PageWithSummaryResponse.of(shareExpenseResponsePage, summary);
    }

    private List<GroupShareExpenseResponseDTO> convertShareExpensesToDTOs(List<Expense> shareExpenses) {
        if (shareExpenses.isEmpty()) {
            return Collections.emptyList();
        }

        List<Expense> originalExpenses = shareExpenses.stream()
                .map(Expense::getOriginalExpense)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        final Map<Long, List<SplitDataDTO>> splitsByOriginalExpenseId;
        if (!originalExpenses.isEmpty()) {
            splitsByOriginalExpenseId = expenseSplitRepository.findByExpenseIn(originalExpenses).stream()
                    .collect(Collectors.groupingBy(
                            split -> split.getExpense().getExpenseId(),
                            Collectors.mapping(
                                    split -> new SplitDataDTO(split.getUser().getUserId(), split.getAmount()),
                                    Collectors.toList()
                            )
                    ));
        } else {
            splitsByOriginalExpenseId = Collections.emptyMap();
        }

        return shareExpenses.stream()
                .map(shareExpense -> {
                    Expense originalExpense = shareExpense.getOriginalExpense();
                    String groupName = shareExpense.getGroup() != null ? shareExpense.getGroup().getGroupName() : "";
                    List<SplitDataDTO> splitData = (originalExpense != null) ?
                            splitsByOriginalExpenseId.getOrDefault(originalExpense.getExpenseId(), Collections.emptyList()) :
                            Collections.emptyList();

                    return expenseConverter.toGroupShareExpenseResponseDTO(
                            shareExpense,
                            originalExpense,
                            groupName,
                            splitData
                    );
                })
                .toList();
    }

    @Override
    public PageWithSummaryResponse<CombinedExpenseResponseDTO> getCombinedExpenses(UUID userId, ExpenseSearchDTO searchDTO, Pageable pageable) {

        User user = findUserById(userId);

        List<CombinedExpenseResponseDTO> personalExpenseDTOs = fetchPersonalExpenseDTOs(user, searchDTO);
        List<CombinedExpenseResponseDTO> shareExpenseDTOs = fetchShareExpenseDTOs(user, searchDTO);

        List<CombinedExpenseResponseDTO> allExpenses = new ArrayList<>();
        allExpenses.addAll(personalExpenseDTOs);
        allExpenses.addAll(shareExpenseDTOs);
        allExpenses.sort(Comparator.comparing(CombinedExpenseResponseDTO::date).reversed());

        ExpenseSummary summary = calculateExpenseSummary(allExpenses);

        Page<CombinedExpenseResponseDTO> page = applyManualPaging(allExpenses, pageable);

        return PageWithSummaryResponse.of(page, summary);
    }

    private List<CombinedExpenseResponseDTO> fetchPersonalExpenseDTOs(User user, ExpenseSearchDTO searchDTO) {
        ExpenseCategory categoryEnum = parseCategory(searchDTO.category());
        List<Expense> personalExpenses = expenseRepository.findPersonalExpensesForCombined(
                user, searchDTO.year(), searchDTO.month(), categoryEnum, searchDTO.search(), Pageable.unpaged()
        ).getContent();

        return personalExpenses.stream()
                .map(expenseConverter::toCombinedResponseDTO)
                .collect(Collectors.toList());
    }

    private List<CombinedExpenseResponseDTO> fetchShareExpenseDTOs(User user, ExpenseSearchDTO searchDTO) {
        ExpenseCategory categoryEnum = parseCategory(searchDTO.category());
        List<Expense> shareExpenses = expenseRepository.findShareExpensesForCombined(
                user, searchDTO.year(), searchDTO.month(), categoryEnum, searchDTO.search(), Pageable.unpaged()
        ).getContent();

        if (shareExpenses.isEmpty()) {
            return Collections.emptyList();
        }

        return shareExpenses.stream()
                .map(shareExpense -> {
                    Expense originalExpense = shareExpense.getOriginalExpense();
                    String groupName = shareExpense.getGroup() != null ? shareExpense.getGroup().getGroupName() : "";
                    return expenseConverter.toCombinedResponseDTO(shareExpense, originalExpense, groupName);
                })
                .collect(Collectors.toList());
    }

    private <T> Page<T> applyManualPaging(List<T> sourceList, Pageable pageable) {
        int start = (int) pageable.getOffset();
        if (start > sourceList.size()) {
            return new PageImpl<>(Collections.emptyList(), pageable, sourceList.size());
        }
        int end = Math.min(start + pageable.getPageSize(), sourceList.size());
        List<T> pageContent = sourceList.subList(start, end);
        return new PageImpl<>(pageContent, pageable, sourceList.size());
    }

    private void validateGroupMembership(User user, Group group) {
        if (!groupMemberRepository.existsByGroupAndUser(group, user)) {
            throw new CustomException(ErrorCode.GROUP_MEMBER_NOT_FOUND);
        }
    }

    private ExpenseCategory parseCategory(String category) {
        if (category == null || category.trim().isEmpty()) {
            return null;
        }
        try {
            return ExpenseCategory.valueOf(category.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new CustomException(ErrorCode.INVALID_CATEGORY);
        }
    }

    private Page<Expense> fetchGroupExpenses(Group group, ExpenseSearchDTO searchDTO, Pageable pageable) {
        String search = searchDTO.search();
        ExpenseCategory categoryEnum = parseCategory(searchDTO.category());
        if (search != null && !search.trim().isEmpty()) {
            return expenseRepository.findGroupExpensesBySearchAndMonthWithPaging(
                    group, search.trim(), searchDTO.year(), searchDTO.month(), pageable
            );
        } else if (categoryEnum != null) {
            return expenseRepository.findGroupExpensesByCategoryAndMonthWithPaging(
                    group, categoryEnum, searchDTO.year(), searchDTO.month(), pageable
            );
        } else {
            return expenseRepository.findGroupExpensesByMonthWithPaging(
                    group, searchDTO.year(), searchDTO.month(), pageable
            );
        }
    }

    private ExpenseSummary calculateExpenseSummary(List<CombinedExpenseResponseDTO> expenses) {
        if (expenses.isEmpty()) {
            return ExpenseSummary.empty();
        }

        BigDecimal totalAmount = expenses.stream()
                .map(CombinedExpenseResponseDTO::myShareAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal averageAmount = totalAmount.divide(
                BigDecimal.valueOf(expenses.size()), 2, RoundingMode.HALF_UP
        );

        CombinedExpenseResponseDTO maxExpense = expenses.stream()
                .max(Comparator.comparing(CombinedExpenseResponseDTO::myShareAmount))
                .orElse(null);

        long totalCount = expenses.size();

        Map<String, CategoryStats> categoryStats = calculateCategoryStats(
                expenses.stream().map(e -> new ExpenseStatItem(e.category().name(), e.myShareAmount())).toList(),
                totalAmount
        );

        return new ExpenseSummary(
                totalAmount,
                averageAmount,
                maxExpense.myShareAmount(),
                totalCount,
                totalAmount,
                totalCount,
                categoryStats,
                maxExpense.expenseId(),
                maxExpense.title()
        );
    }

    private Map<String, CategoryStats> calculateCategoryStats(List<ExpenseStatItem> items, BigDecimal totalAmount) {
        Map<String, List<ExpenseStatItem>> categoryGroups = items.stream()
                .collect(Collectors.groupingBy(ExpenseStatItem::category));

        return categoryGroups.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> {
                            List<ExpenseStatItem> categoryExpenses = entry.getValue();

                            BigDecimal categoryAmount = categoryExpenses.stream()
                                    .map(ExpenseStatItem::amount)
                                    .reduce(BigDecimal.ZERO, BigDecimal::add);

                            long categoryCount = categoryExpenses.size();

                            BigDecimal percentage = totalAmount.compareTo(BigDecimal.ZERO) == 0
                                    ? BigDecimal.ZERO
                                    : categoryAmount.divide(totalAmount, 4, RoundingMode.HALF_UP)
                                    .multiply(BigDecimal.valueOf(100));

                            return new CategoryStats(categoryAmount, categoryCount, percentage);
                        }
                ));
    }

    private record ExpenseStatItem(String category, BigDecimal amount) {
    }

    @Override
    public PageWithSummaryResponse<ExpenseResponseDTO> getGroupExpensesWithStats(
            UUID userId, UUID groupId, ExpenseSearchDTO searchDTO, Pageable pageable
    ) {
        User user = findUserById(userId);
        Group group = findGroupById(groupId);

        validateGroupMembership(user, group);

        Page<Expense> groupExpensesPage = fetchGroupExpenses(group, searchDTO, pageable);

        if (!groupExpensesPage.hasContent()) {
            return PageWithSummaryResponse.of(Page.empty(pageable), ExpenseSummary.empty());
        }

        List<ExpenseResponseDTO> pageContent = convertToExpenseResponseDTOs(groupExpensesPage.getContent());
        Page<ExpenseResponseDTO> expenseResponsePage = new PageImpl<>(pageContent, pageable, groupExpensesPage.getTotalElements());

        List<Expense> allExpensesForStats = fetchGroupExpenses(group, searchDTO, Pageable.unpaged()).getContent();
        List<ExpenseResponseDTO> allExpenseDTOsForStats = convertToExpenseResponseDTOs(allExpensesForStats);

        ExpenseSummary summary = calculateGroupExpensesSummary(allExpenseDTOsForStats);

        return PageWithSummaryResponse.of(expenseResponsePage, summary);
    }

    private List<ExpenseResponseDTO> convertToExpenseResponseDTOs(List<Expense> expenses) {
        if (expenses == null || expenses.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, List<SplitDataDTO>> splitsByExpenseId = expenseSplitRepository.findByExpenseIn(expenses)
                .stream()
                .collect(Collectors.groupingBy(
                        split -> split.getExpense().getExpenseId(),
                        Collectors.mapping(split -> new SplitDataDTO(
                                        split.getUser().getUserId(), split.getAmount())
                                , Collectors.toList()
                        )
                ));

        return expenses.stream()
                .map(expense -> {
                    List<SplitDataDTO> splitData = splitsByExpenseId.getOrDefault(expense.getExpenseId(), Collections.emptyList());
                    return expenseConverter.toResponseDTO(expense, splitData);
                })
                .collect(Collectors.toList());
    }

    private ExpenseSummary calculateGroupExpensesSummary(List<ExpenseResponseDTO> expenses) {
        if (expenses.isEmpty()) {
            return ExpenseSummary.empty();
        }

        BigDecimal totalAmount = expenses.stream()
                .map(ExpenseResponseDTO::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal averageAmount = totalAmount.divide(
                BigDecimal.valueOf(expenses.size()), 2, RoundingMode.HALF_UP
        );

        ExpenseResponseDTO maxExpense = expenses.stream()
                .max(Comparator.comparing(ExpenseResponseDTO::amount))
                .orElse(null);

        long totalCount = expenses.size();

        Map<String, CategoryStats> categoryStats = calculateCategoryStats(
                expenses.stream().map(e -> new ExpenseStatItem(e.category().name(), e.amount())).toList(),
                totalAmount
        );

        return new ExpenseSummary(
                totalAmount,
                averageAmount,
                maxExpense.amount(),
                totalCount,
                totalAmount,
                totalCount,
                categoryStats,
                maxExpense.expenseId(),
                maxExpense.title()
        );
    }

}
