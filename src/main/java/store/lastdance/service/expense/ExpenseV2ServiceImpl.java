package store.lastdance.service.expense;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
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

    /**
     * 개인 지출 등록
     */
    @Override
    public ExpenseResponseDTO createPersonalExpense(UUID userId, CreatePersonalExpenseRequestDTO requestDTO, MultipartFile receiptFile) {
        Expense expense = createBaseExpense(userId, requestDTO, ExpenseType.PERSONAL, receiptFile);

        Expense savedExpense = expenseRepository.save(expense);
        return ExpenseResponseDTO.from(savedExpense);
    }

    /**
     * 그룹 지출 등록
     */
    @Override
    public ExpenseResponseDTO createGroupExpense(UUID userId, CreateGroupExpenseRequestDTO requestDTO, MultipartFile receiptFile) {
        Expense expense = createBaseExpense(userId, requestDTO, ExpenseType.GROUP, receiptFile);

        // 그룹 정보 설정
        Group group = findGroupById(requestDTO.groupId());
        expense.updateGroup(group);
        expense.updateSplitType(requestDTO.splitType());

        Expense savedExpense = expenseRepository.save(expense);

        // 그룹 지출 정산 처리
        processGroupExpenseSplit(savedExpense, requestDTO);

        return ExpenseResponseDTO.from(savedExpense);
    }

    /**
     * 공통 지출 등록
     */
    private Expense createBaseExpense(
            UUID userId, BaseExpenseRequest request, ExpenseType expenseType, MultipartFile receiptFile
    ) {
        ImageFile uploadedImage = null;

        try {
            // 영수증 파일 업로드 처리
            if (receiptFile != null && !receiptFile.isEmpty()) {
                uploadedImage = imageService.uploadImageToS3(receiptFile, "receipt-image", 10 * 1024 * 1024);
            }
            User user = findUserById(userId);

            // 기본 지출 생성
            Expense expense = Expense.builder()
                    .title(request.title())
                    .amount(request.amount())
                    .category(request.category())
                    .expenseType(expenseType)
                    .user(user)
                    .expenseDate(request.date())
                    .build();

            // 영수증 파일 ID 설정
            if (uploadedImage != null) {
                expense.updateReceiptImageFile(uploadedImage);
            }
            // 메모 설정
            if (request.memo() != null) {
                expense.updateMemo(request.memo());
            }
            return expense;

        } catch (Exception e) {
            // S3 파일 정리
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

    /**
     * 그룹 지출 정산 처리
     */
    private void processGroupExpenseSplit(Expense original, CreateGroupExpenseRequestDTO dto) {
        // 그룹 멤버 조회
        List<GroupMember> groupMembers = validateAndGetGroupMembers(original);
        switch (dto.splitType()) {
            case EQUAL -> processEqualSplit(original, groupMembers);
            case CUSTOM, SPECIFIC -> processCustomSplit(original, dto.splitData());
        }
    }

    /**
     * 커스텀/비율 분할 처리
     */
    private void processCustomSplit(Expense original, List<SplitDataDTO> splitData) {
        if (splitData == null || splitData.isEmpty()) {
            throw new CustomException(ErrorCode.SPLIT_DATA_REQUIRED);
        }

        for (SplitDataDTO split : splitData) {
            User user = findUserById(split.userId());

            ExpenseSplit expenseSplit = ExpenseSplit.builder()
                    .expense(original)
                    .user(user)
                    .amount(split.amount())
                    .build();
            expenseSplitRepository.save(expenseSplit);

            createShareExpense(original, user, split.amount());
        }
    }

    /**
     * 균등 분할 처리 (원화 기준)
     */
    private void processEqualSplit(Expense original, List<GroupMember> members) {
        BigDecimal totalAmount = original.getAmount();
        int memberCount = members.size();

        if (memberCount == 0) {
            return; // 처리할 멤버가 없으면 종료
        }

        // 1. 1인당 기본 분담금 계산 (정수 단위, 버림)
        BigDecimal baseSplitAmount = totalAmount.divide(BigDecimal.valueOf(memberCount), 0, RoundingMode.DOWN);

        // 2. 나누고 남은 나머지 금액 계산
        BigDecimal calculatedTotal = baseSplitAmount.multiply(BigDecimal.valueOf(memberCount));
        BigDecimal remainder = totalAmount.subtract(calculatedTotal);

        // 3. 나머지 금액(원)을 분배할 횟수 계산
        int remainderToDistribute = remainder.intValue();

        // 4. 각 멤버에게 분담금 할당
        for (int i = 0; i < memberCount; i++) {
            GroupMember member = members.get(i);
            BigDecimal finalSplitAmount = baseSplitAmount;

            // 5. 나머지 금액을 순서대로 1원씩 분배
            if (i < remainderToDistribute) {
                finalSplitAmount = finalSplitAmount.add(BigDecimal.ONE);
            }

            // ExpenseSplit 생성
            ExpenseSplit split = ExpenseSplit.builder()
                    .expense(original)
                    .user(member.getUser())
                    .amount(finalSplitAmount)
                    .build();
            expenseSplitRepository.save(split);

            createShareExpense(original, member.getUser(), finalSplitAmount);
        }
    }

    /**
     * 개인 가계부에 분담 지출 생성
     */
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

        // 원본 지출 id 설정
        shareExpense.updateOriginalExpense(original);
        expenseRepository.save(shareExpense);
    }

    /**
     * 지출 정산 데이터 조회
     */
    private List<SplitDataDTO> getSplitData(Expense expense) {
        return expenseSplitRepository.findByExpense(expense)
                .stream()
                .map(split -> new SplitDataDTO(split.getUser().getUserId(), split.getAmount()))
                .toList();
    }


    /**
     * 지출 수정
     */
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

            // 영수증 파일 처리
            if (receiptFile != null && !receiptFile.isEmpty()) {
                // 새 영수증 업로드
                uploadedImage = imageService.uploadImageToS3(receiptFile, "receipt-image", 10 * 1024 * 1024);
                expense.updateReceiptImageFile(uploadedImage);

                // 기존 영수증 삭제 (새 파일 업로드 성공 후)
                if (oldReceiptFileId != null) {
                    imageService.deleteImageFromS3(oldReceiptFileId);
                }
            }

            // 그룹 지출의 경우 분담 정보 업데이트
            if (expense.getExpenseType() == ExpenseType.GROUP && expense.getSplitType() != null) {
                expense.updateSplitType(requestDTO.splitType());
                updateGroupExpenseSplits(expense, requestDTO.splitData());
            }
        } catch (Exception e) {
            // 새로 업로드한 파일 정리 (업데이트 실패 시)
            if (uploadedImage != null) {
                try {
                    imageService.deleteImageFromS3(uploadedImage.getFileId());
                } catch (Exception deleteEx) {
                    log.error("지출수정_고아파일 정리 실패: {}", deleteEx.getMessage());
                }
            }
            throw e;
        }

        return ExpenseResponseDTO.from(expense);
    }

    /**
     * 그룹 지출 분담 정보 업데이트
     */
    private void updateGroupExpenseSplits(Expense original, List<SplitDataDTO> newSplitData) {
        // 1. 기존 분담 데이터 모두 삭제
        expenseSplitRepository.deleteByExpense(original);
        expenseRepository.deleteByOriginalExpense(original);

        // 2. 그룹 멤버 정보 다시 조회
        List<GroupMember> groupMembers = validateAndGetGroupMembers(original);

        // 3. 새로운 분할 방식에 따라 분담금 재생성
        switch (original.getSplitType()) {
            case EQUAL -> processEqualSplit(original, groupMembers);
            case CUSTOM, SPECIFIC -> processCustomSplit(original, newSplitData);
        }
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

    /**
     * 지출 삭제
     */
    @Override
    public void deleteExpense(UUID userId, Long expenseId) {

        User user = findUserById(userId);

        Expense expense = expenseRepository.findByExpenseIdWithPermission(expenseId, user).orElseThrow(
                () -> new CustomException(ErrorCode.EXPENSE_NOT_FOUND)
        );

        // 그룹 지출인 경우 연관 데이터 정리
        if (expense.getExpenseType() == ExpenseType.GROUP) {
            expenseSplitRepository.deleteByExpense(expense);
            expenseRepository.deleteByOriginalExpense(expense);
        }

        expenseRepository.deleteById(expenseId);
    }


    /**
     * 영수증만 삭제 (지출은 유지)
     */
    @Override
    public void deleteReceiptImage(Long expenseId, UUID userId) {

        User user = findUserById(userId);

        Expense expense = expenseRepository.findByExpenseIdWithPermission(expenseId, user).orElseThrow(
                () -> new CustomException(ErrorCode.EXPENSE_NOT_FOUND)
        );

        // 영수증이 없으면 예외
        ImageFile receiptImageFile = expense.getReceiptImageFile();
        if (receiptImageFile == null) {
            throw new CustomException(ErrorCode.FILE_NOT_FOUND);
        }

        // S3에서 파일 삭제
        imageService.deleteImageFromS3(receiptImageFile.getFileId());

        expense.updateReceiptImageFile(null);
    }


    /**
     * LLM 지출 분석
     *
     * @param userId     사용자의 ID
     * @param requestDTO 시작일, 종료일
     * @return 4가지 유형 - 예산 사용률, 일 평균 지출(월말 예상), 분석 결과, 카테고리별 상세 분석
     */
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

    /**
     * LLM 지출 분석 내역 저장
     */
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

    /**
     * LLM 지출 분석 피드백
     */
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
        // 현재 상태와 같은 버튼을 다시 누르면 피드백 취소
        if ((isUp && Boolean.TRUE.equals(history.getUp())) || (isDown && Boolean.TRUE.equals(history.getDown()))) {
            history.feedback(null, null);
            return "CANCELED";
        } else {
            // 새로운 피드백 설정
            history.feedback(isUp, isDown);
            return "APPLIED";
        }
    }

    /**
     * 사용자의 예산 정보를 조회합니다.
     *
     * @param userId 사용자의 UUID
     * @return 사용자의 예산
     */
    private BigDecimal getUserBudget(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        return BigDecimal.valueOf(user.getUserBudget());
    }

    /**
     * 지출 내역 리스트를 받아 총 지출액 계산
     */
    private BigDecimal calculateTotalSpending(List<Expense> expenses) {
        return expenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 예산 사용률 객체 생성
     */
    private AnalyzeExpenseResponseDTO.BudgetUsage calculateBudgetUsage(BigDecimal totalSpending, BigDecimal totalBudget) {
        if (totalBudget.compareTo(BigDecimal.ZERO) == 0) {
            return new AnalyzeExpenseResponseDTO.BudgetUsage(0.0, totalSpending, totalBudget);
        }
        BigDecimal percentage = totalSpending.multiply(BigDecimal.valueOf(100)).divide(totalBudget, 2, RoundingMode.HALF_UP);
        return new AnalyzeExpenseResponseDTO.BudgetUsage(percentage.doubleValue(), totalSpending, totalBudget);
    }

    /**
     * 일평균, 월말 지출 객체 생성
     */
    private AnalyzeExpenseResponseDTO.DailySpending calculateDailySpending(BigDecimal totalSpending, LocalDate startDate, LocalDate endDate) {
        long days = java.time.temporal.ChronoUnit.DAYS.between(startDate, LocalDate.now()) + 1;
        BigDecimal averageSoFar = totalSpending.divide(BigDecimal.valueOf(days), 0, RoundingMode.HALF_UP);
        int daysInMonth = endDate.lengthOfMonth();
        BigDecimal estimatedEom = averageSoFar.multiply(BigDecimal.valueOf(daysInMonth)); // 월말 예상 지출

        return new AnalyzeExpenseResponseDTO.DailySpending(averageSoFar, estimatedEom);
    }

    /**
     * 카테고리별 상세 분석 객체 리스트 생성
     */
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

    /**
     * LLM으로부터 개선 제안을 받아옴
     */
    private AnalyzeExpenseResponseDTO.Suggestion getLlmAnalysisResult(List<Expense> expenses) {
        // LLM에 전달할 데이터만 동적으로 가공
        List<Map<String, Object>> llmExpenseData = expenses.stream()
                .map(expense -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("title", expense.getTitle());
                    data.put("amount", expense.getAmount());
                    data.put("category", expense.getCategory().getDescription()); // 한글 설명
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

    /**
     * 분석 데이터 없을 때 반환할 기본 DTO
     */
    private AnalyzeExpenseResponseDTO createEmptyAnalysis(BigDecimal totalBudget) {
        AnalyzeExpenseResponseDTO.BudgetUsage budgetUsage = new AnalyzeExpenseResponseDTO.BudgetUsage(0.0, BigDecimal.ZERO, totalBudget);
        AnalyzeExpenseResponseDTO.DailySpending dailySpending = new AnalyzeExpenseResponseDTO.DailySpending(BigDecimal.ZERO, BigDecimal.ZERO);

        AnalyzeExpenseResponseDTO.AnalysisResult analysisResult = new AnalyzeExpenseResponseDTO.AnalysisResult("분석할 지출 내역이 없습니다.",
                new AnalyzeExpenseResponseDTO.Suggestion("지출 내역을 추가하고 다시 시도해주세요.", "없음", "없음", "없음"));

        return new AnalyzeExpenseResponseDTO(null, budgetUsage, dailySpending, analysisResult, Collections.emptyList());
    }

    /**
     * 가장 지출이 큰 카테고리를 찾아 분석요약(mainFinding) 생성
     */
    private String createMainFinding(List<AnalyzeExpenseResponseDTO.CategoryDetail> categoryDetails) {
        if (categoryDetails == null || categoryDetails.isEmpty()) {
            return "주요 지출 항목이 없습니다.";
        }
        AnalyzeExpenseResponseDTO.CategoryDetail maxCategoryDetail = categoryDetails.get(0);

        return String.format("%s 지출 집중", maxCategoryDetail.category());
    }

}



