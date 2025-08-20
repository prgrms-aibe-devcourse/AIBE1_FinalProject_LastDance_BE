package store.lastdance.service.expense;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import store.lastdance.converter.expense.ExpenseConverter;
import store.lastdance.domain.common.ImageFile;
import store.lastdance.domain.expense.*;
import store.lastdance.domain.group.Group;
import store.lastdance.domain.group.GroupMember;
import store.lastdance.domain.user.User;
import store.lastdance.dto.expense.*;
import store.lastdance.exception.CustomException;
import store.lastdance.exception.ErrorCode;
import store.lastdance.repository.expense.ExpenseAnalysisHistoryRepository;
import store.lastdance.repository.expense.ExpenseRepository;
import store.lastdance.repository.expense.ExpenseSplitRepository;
import store.lastdance.repository.group.GroupMemberRepository;
import store.lastdance.repository.group.GroupRepository;
import store.lastdance.repository.user.UserRepository;
import store.lastdance.service.image.ImageService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ExpenseV2ServiceImpl implements ExpenseV2Service {
    private final ExpenseRepository expenseRepository;
    private final ExpenseSplitRepository expenseSplitRepository;
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;
    private final ExpenseAnalysisHistoryRepository expenseAnalysisHistoryRepository;
    private final ImageService imageService;
    private final ObjectMapper objectMapper;
    private final ExpenseAnalyzer expenseAnalyzer;
    private final ExpenseSplitter expenseSplitter;
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
    public ExpenseResponseDTO createPersonalExpense(UUID userId, CreatePersonalExpenseRequestDTO requestDTO, MultipartFile receiptFile) {
        Expense expense = createBaseExpense(userId, requestDTO, ExpenseType.PERSONAL, receiptFile);

        Expense savedExpense = expenseRepository.save(expense);
        return expenseConverter.toResponseDTO(savedExpense);
    }

    @Override
    public ExpenseResponseDTO createGroupExpense(UUID userId, CreateGroupExpenseRequestDTO requestDTO, MultipartFile receiptFile) {
        Expense expense = createBaseExpense(userId, requestDTO, ExpenseType.GROUP, receiptFile);

        Group group = findGroupById(requestDTO.groupId());
        expense.updateGroup(group);
        expense.updateSplitType(requestDTO.splitType());

        Expense savedExpense = expenseRepository.save(expense);
        processGroupExpenseSplit(savedExpense, requestDTO);
        return expenseConverter.toResponseDTO(savedExpense);
    }

    private Expense createBaseExpense(
            UUID userId, BaseExpenseRequest request, ExpenseType expenseType, MultipartFile receiptFile
    ) {
        ImageFile uploadedImage = null;

        try {
            if (receiptFile != null && !receiptFile.isEmpty()) {
                uploadedImage = imageService.uploadImageToS3(receiptFile, "receipt-image", 10 * 1024 * 1024);
            }
            User user = findUserById(userId);

            Expense expense = Expense.builder()
                    .title(request.title())
                    .amount(request.amount())
                    .category(request.category())
                    .expenseType(expenseType)
                    .user(user)
                    .expenseDate(request.date())
                    .build();

            if (uploadedImage != null) {
                expense.updateReceiptImageFile(uploadedImage);
            }
            if (request.memo() != null) {
                expense.updateMemo(request.memo());
            }
            return expense;

        } catch (Exception e) {
            if (uploadedImage != null) {
                try {
                    imageService.deleteImageFromS3(uploadedImage.getFileId());
                } catch (Exception deleteEx) {
                    log.error("고아파일 정리 실패: {}", deleteEx.getMessage());
                }
            }
            throw e;
        }
    }

    private void processGroupExpenseSplit(Expense original, CreateGroupExpenseRequestDTO dto) {
        List<GroupMember> groupMembers = validateAndGetGroupMembers(original);
        List<User> members = groupMembers.stream().map(GroupMember::getUser).toList();

        Map<User, BigDecimal> splitAmountMap = expenseSplitter.split(
                dto.splitType(),
                original.getAmount(),
                members,
                dto.splitData()
        );

        applySplitResultToDatabase(original, splitAmountMap);
    }

    private void createShareExpense(Expense original, User user, BigDecimal shareAmount) {
        Expense shareExpense = Expense.builder()
                .title(original.getTitle() + " (그룹 분담)")
                .amount(shareAmount)
                .category(original.getCategory())
                .expenseType(ExpenseType.SHARE)
                .user(user)
                .expenseDate(original.getExpenseDate())
                .build();

        shareExpense.updateGroup(original.getGroup());
        shareExpense.updateMemo(original.getMemo());

        shareExpense.updateOriginalExpense(original);
        expenseRepository.save(shareExpense);
    }

    private void applySplitResultToDatabase(Expense original, Map<User, BigDecimal> splitAmountMap) {
        for (Map.Entry<User, BigDecimal> entry : splitAmountMap.entrySet()) {
            User user = entry.getKey();
            BigDecimal amount = entry.getValue();

            ExpenseSplit expenseSplit = ExpenseSplit.builder()
                    .expense(original)
                    .user(user)
                    .amount(amount)
                    .build();
            expenseSplitRepository.save(expenseSplit);

            createShareExpense(original, user, amount);
        }
    }

    @Override
    public ExpenseResponseDTO updateExpense(UUID userId, Long expenseId, UpdateExpenseRequestDTO requestDTO, MultipartFile receiptFile) {

        User user = findUserById(userId);

        Expense expense = expenseRepository.findByExpenseIdWithPermission(expenseId, user).orElseThrow(
                () -> new CustomException(ErrorCode.EXPENSE_NOT_FOUND)
        );

        ImageFile oldReceiptImageFile = expense.getReceiptImageFile();
        UUID oldReceiptFileId = oldReceiptImageFile != null ? oldReceiptImageFile.getFileId() : null;

        ImageFile uploadedImage = null;

        try {
            expense.updateTitle(requestDTO.title());
            expense.updateAmount(requestDTO.amount());
            expense.updateCategory(requestDTO.category());
            expense.updateMemo(requestDTO.memo());
            expense.updateExpenseDate(requestDTO.date());

            if (receiptFile != null && !receiptFile.isEmpty()) {
                uploadedImage = imageService.uploadImageToS3(receiptFile, "receipt-image", 10 * 1024 * 1024);
                expense.updateReceiptImageFile(uploadedImage);

                if (oldReceiptFileId != null) {
                    imageService.deleteImageFromS3(oldReceiptFileId);
                }
            }

            if (expense.getExpenseType() == ExpenseType.GROUP && expense.getSplitType() != null) {
                expense.updateSplitType(requestDTO.splitType());
                updateGroupExpenseSplits(expense, requestDTO.splitData());
            }
        } catch (Exception e) {
            if (uploadedImage != null) {
                try {
                    imageService.deleteImageFromS3(uploadedImage.getFileId());
                } catch (Exception deleteEx) {
                    log.error("지출수정_고아파일 정리 실패: {}", deleteEx.getMessage());
                }
            }
            throw e;
        }

        return expenseConverter.toResponseDTO(expense);
    }

    private void updateGroupExpenseSplits(Expense original, List<SplitDataDTO> newSplitData) {
        expenseSplitRepository.deleteByExpense(original);
        expenseRepository.deleteByOriginalExpense(original);

        List<GroupMember> groupMembers = validateAndGetGroupMembers(original);
        List<User> members = groupMembers.stream().map(GroupMember::getUser).toList();

        Map<User, BigDecimal> splitAmountMap = expenseSplitter.split(
                original.getSplitType(),
                original.getAmount(),
                members,
                newSplitData
        );

        applySplitResultToDatabase(original, splitAmountMap);
    }

    private List<GroupMember> validateAndGetGroupMembers(Expense expense) {
        Group group = expense.getGroup();
        if (group == null) {
            throw new CustomException(ErrorCode.GROUP_NOT_FOUND);
        }

        List<GroupMember> groupMembers = groupMemberRepository.findByGroup(group);
        if (groupMembers.isEmpty()) {
            throw new CustomException(ErrorCode.GROUP_MEMBER_NOT_FOUND);
        }

        return groupMembers;
    }

    @Override
    public void deleteExpense(UUID userId, Long expenseId) {

        User user = findUserById(userId);

        Expense expense = expenseRepository.findByExpenseIdWithPermission(expenseId, user).orElseThrow(
                () -> new CustomException(ErrorCode.EXPENSE_NOT_FOUND)
        );

        if (expense.getExpenseType() == ExpenseType.GROUP) {
            expenseSplitRepository.deleteByExpense(expense);
            expenseRepository.deleteByOriginalExpense(expense);
        }

        expenseRepository.deleteById(expenseId);
    }

    @Override
    public void deleteReceiptImage(Long expenseId, UUID userId) {

        User user = findUserById(userId);

        Expense expense = expenseRepository.findByExpenseIdWithPermission(expenseId, user).orElseThrow(
                () -> new CustomException(ErrorCode.EXPENSE_NOT_FOUND)
        );

        ImageFile receiptImageFile = expense.getReceiptImageFile();
        if (receiptImageFile == null) {
            throw new CustomException(ErrorCode.FILE_NOT_FOUND);
        }

        imageService.deleteImageFromS3(receiptImageFile.getFileId());

        expense.updateReceiptImageFile(null);
    }

    @Override
    public AnalyzeExpenseResponseDTO analyzeExpenses(UUID userId, AnalyzeExpenseRequestDTO requestDTO) {
        User user = userRepository.findById(userId).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        List<Expense> expenses = expenseRepository.findPersonalAndShareExpensesByDateRange(user, requestDTO.startDate(), requestDTO.endDate());
        BigDecimal totalBudget = getUserBudget(userId);

        if (expenses.isEmpty()) {
            return createEmptyAnalysis(totalBudget);
        }

        BigDecimal totalSpending = calculateTotalSpending(expenses);

        AnalyzeExpenseResponseDTO.BudgetUsage budgetUsage = calculateBudgetUsage(totalSpending, totalBudget);
        AnalyzeExpenseResponseDTO.DailySpending dailySpending = calculateDailySpending(totalSpending, requestDTO.startDate(), requestDTO.endDate());

        List<AnalyzeExpenseResponseDTO.CategoryDetail> categoryDetails = calculateCategoryDetails(expenses, totalSpending);

        AnalyzeExpenseResponseDTO.Suggestion suggestion = getLlmAnalysisResult(expenses);

        String mainFinding = createMainFinding(categoryDetails);

        AnalyzeExpenseResponseDTO.AnalysisResult analysisResult = new AnalyzeExpenseResponseDTO.AnalysisResult(mainFinding, suggestion);

        ExpenseAnalysisHistory savedHistory = saveAnalysisHistory(user, requestDTO, budgetUsage, dailySpending, analysisResult);

        return new AnalyzeExpenseResponseDTO(savedHistory.getId(), budgetUsage, dailySpending, analysisResult, categoryDetails);
    }

    private ExpenseAnalysisHistory saveAnalysisHistory(User user, AnalyzeExpenseRequestDTO requestDTO, AnalyzeExpenseResponseDTO.BudgetUsage budgetUsage, AnalyzeExpenseResponseDTO.DailySpending dailySpending, AnalyzeExpenseResponseDTO.AnalysisResult analysisResult) {

        ExpenseAnalysisHistory history = ExpenseAnalysisHistory.builder()
                .user(user)
                .startDate(requestDTO.startDate())
                .endDate(requestDTO.endDate())
                .budgetUsagePercentage(budgetUsage.percentage())
                .budgetUsageCurrentSpending(budgetUsage.currentSpending())
                .budgetUsageTotalBudget(budgetUsage.totalBudget())
                .dailySpendingAverageSoFar(dailySpending.averageSoFar())
                .dailySpendingEstimatedEom(dailySpending.estimatedEom())
                .mainFinding(analysisResult.mainFinding())
                .suggestionTitle(analysisResult.suggestion().title())
                .suggestionDescription(analysisResult.suggestion().description())
                .suggestionEffect(analysisResult.suggestion().effect())
                .suggestionDifficulty(analysisResult.suggestion().difficulty())
                .build();

        return expenseAnalysisHistoryRepository.save(history);
    }

    @Override
    public String toggleFeedback(Long historyId, UUID userid, String type) {
        ExpenseAnalysisHistory history = expenseAnalysisHistoryRepository.findById(historyId)
                .orElseThrow(() -> new CustomException(ErrorCode.HISTORY_NOT_FOUND));

        if (!history.getUser().getUserId().equals(userid)) {
            throw new CustomException(ErrorCode.EXPENSE_ACCESS_DENIED);
        }

        boolean isUp = "up".equals(type);
        boolean isDown = "down".equals(type);

        if (!isUp && !isDown) {
            throw new CustomException(ErrorCode.INVALID_HISTORY_REQUEST);
        }
        if ((isUp && Boolean.TRUE.equals(history.getUp())) || (isDown && Boolean.TRUE.equals(history.getDown()))) {
            history.feedback(null, null);
            return "CANCELED";
        } else {
            history.feedback(isUp, isDown);
            return "APPLIED";
        }
    }

    private BigDecimal getUserBudget(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        return BigDecimal.valueOf(user.getUserBudget());
    }

    private BigDecimal calculateTotalSpending(List<Expense> expenses) {
        return expenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private AnalyzeExpenseResponseDTO.BudgetUsage calculateBudgetUsage(BigDecimal totalSpending, BigDecimal totalBudget) {
        if (totalBudget.compareTo(BigDecimal.ZERO) == 0) {
            return new AnalyzeExpenseResponseDTO.BudgetUsage(0.0, totalSpending, totalBudget);
        }
        BigDecimal percentage = totalSpending.multiply(BigDecimal.valueOf(100)).divide(totalBudget, 2, RoundingMode.HALF_UP);
        return new AnalyzeExpenseResponseDTO.BudgetUsage(percentage.doubleValue(), totalSpending, totalBudget);
    }

    private AnalyzeExpenseResponseDTO.DailySpending calculateDailySpending(BigDecimal totalSpending, LocalDate startDate, LocalDate endDate) {
        long days = java.time.temporal.ChronoUnit.DAYS.between(startDate, LocalDate.now()) + 1;
        BigDecimal averageSoFar = totalSpending.divide(BigDecimal.valueOf(days), 0, RoundingMode.HALF_UP);
        int daysInMonth = endDate.lengthOfMonth();
        BigDecimal estimatedEom = averageSoFar.multiply(BigDecimal.valueOf(daysInMonth));

        return new AnalyzeExpenseResponseDTO.DailySpending(averageSoFar, estimatedEom);
    }

    private List<AnalyzeExpenseResponseDTO.CategoryDetail> calculateCategoryDetails(List<Expense> expenses, BigDecimal totalSpending) {
        if (totalSpending.compareTo(BigDecimal.ZERO) == 0) {
            return Collections.emptyList();
        }

        Map<ExpenseCategory, List<Expense>> expensesByCategory = expenses.stream()
                .collect(Collectors.groupingBy(Expense::getCategory));

        return expensesByCategory.entrySet().stream()
                .map(entry -> {
                    ExpenseCategory category = entry.getKey();
                    List<Expense> categoryExpenses = entry.getValue();
                    BigDecimal categoryTotal = calculateTotalSpending(categoryExpenses);
                    double percentage = categoryTotal.divide(totalSpending, 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100)).doubleValue();
                    int count = categoryExpenses.size();
                    return new AnalyzeExpenseResponseDTO.CategoryDetail(category.getDescription(), percentage, categoryTotal, count);
                }).sorted(Comparator.comparing(AnalyzeExpenseResponseDTO.CategoryDetail::percentage).reversed()).toList();
    }

    private AnalyzeExpenseResponseDTO.Suggestion getLlmAnalysisResult(List<Expense> expenses) {
        List<Map<String, Object>> llmExpenseData = expenses.stream()
                .map(expense -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("title", expense.getTitle());
                    data.put("amount", expense.getAmount());
                    data.put("category", expense.getCategory().getDescription());
                    data.put("expenseType", expense.getExpenseType().getDescription());
                    if (expense.getSplitType() != null) {
                        data.put("splitType", expense.getSplitType().getDescription());
                    }
                    data.put("date", expense.getExpenseDate());
                    data.put("memo", expense.getMemo());
                    return data;
                })
                .toList();
        try {
            String expenseJson = objectMapper.writeValueAsString(llmExpenseData);
            return expenseAnalyzer.analyzerExpenseData(expenseJson);
        } catch (JsonProcessingException e) {
            log.error("LLM 전송용 DTO to JSON 변환 실패");
            return new AnalyzeExpenseResponseDTO.Suggestion("데이터 처리중 오류 발생", "잠시 후 다시 시도해주세요.", "오류", "오류");
        }
    }

    private AnalyzeExpenseResponseDTO createEmptyAnalysis(BigDecimal totalBudget) {
        AnalyzeExpenseResponseDTO.BudgetUsage budgetUsage = new AnalyzeExpenseResponseDTO.BudgetUsage(0.0, BigDecimal.ZERO, totalBudget);
        AnalyzeExpenseResponseDTO.DailySpending dailySpending = new AnalyzeExpenseResponseDTO.DailySpending(BigDecimal.ZERO, BigDecimal.ZERO);

        AnalyzeExpenseResponseDTO.AnalysisResult analysisResult = new AnalyzeExpenseResponseDTO.AnalysisResult("분석할 지출 내역이 없습니다.",
                new AnalyzeExpenseResponseDTO.Suggestion("지출 내역을 추가하고 다시 시도해주세요.", "없음", "없음", "없음"));

        return new AnalyzeExpenseResponseDTO(null, budgetUsage, dailySpending, analysisResult, Collections.emptyList());
    }

    private String createMainFinding(List<AnalyzeExpenseResponseDTO.CategoryDetail> categoryDetails) {
        if (categoryDetails == null || categoryDetails.isEmpty()) {
            return "주요 지출 항목이 없습니다.";
        }
        AnalyzeExpenseResponseDTO.CategoryDetail maxCategoryDetail = categoryDetails.get(0);

        return String.format("%s 지출 집중", maxCategoryDetail.category());
    }

}



