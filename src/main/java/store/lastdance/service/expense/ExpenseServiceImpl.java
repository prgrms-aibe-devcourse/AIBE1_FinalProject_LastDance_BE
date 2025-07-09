package store.lastdance.service.expense;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import store.lastdance.domain.common.ImageFile;
import store.lastdance.domain.expense.*;
import store.lastdance.domain.group.Group;
import store.lastdance.domain.group.GroupMember;
import store.lastdance.domain.user.User;
import store.lastdance.dto.expense.*;
import store.lastdance.dto.response.PageWithSummaryResponse;
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
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ExpenseServiceImpl implements ExpenseService {
    private final ExpenseRepository expenseRepository;
    private final ExpenseSplitRepository expenseSplitRepository;
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;
    private final ExpenseAnalysisHistoryRepository expenseAnalysisHistoryRepository;
    private final ImageService imageService;
    private final ObjectMapper objectMapper;
    private final ExpenseAnalyzer expenseAnalyzer;

    /**
     * 개인 지출 등록
     */
    @Override
    @Transactional
    public ExpenseResponseDTO createPersonalExpense(UUID userId, CreatePersonalExpenseRequestDTO requestDTO, MultipartFile receiptFile) {
        Expense expense = createBaseExpense(userId, requestDTO, ExpenseType.PERSONAL, receiptFile);

        Expense savedExpense = expenseRepository.save(expense);
        return ExpenseResponseDTO.from(savedExpense);
    }

    /**
     * 그룹 지출 등록
     */
    @Override
    @Transactional
    public ExpenseResponseDTO createGroupExpense(UUID userId, CreateGroupExpenseRequestDTO requestDTO, MultipartFile receiptFile) {
        Expense expense = createBaseExpense(userId, requestDTO, ExpenseType.GROUP, receiptFile);

        // 그룹 정보 설정
        expense.setGroupId(requestDTO.groupId());
        expense.setSplitType(requestDTO.splitType());

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
        UUID receiptFileId = null;

        try {
            // 영수증 파일 업로드 처리
            if (receiptFile != null && !receiptFile.isEmpty()) {
                ImageFile uploadedImage = imageService.uploadImageToS3(receiptFile, "receipt-image", 10 * 1024 * 1024);
                receiptFileId = uploadedImage.getFileId();
            }

            // 기본 지출 생성
            Expense expense = Expense.builder()
                    .title(request.title())
                    .amount(request.amount())
                    .category(request.category())
                    .expenseType(expenseType)
                    .userId(userId)
                    .expenseDate(request.date())
                    .build();

            // 영수증 파일 ID 설정
            if (receiptFileId != null) {
                expense.addReceiptImage(receiptFileId);
            }
            // 메모 설정
            if (request.memo() != null) {
                expense.updateMemo(request.memo());
            }
            return expense;

        } catch (Exception e) {
            // S3 파일 정리
            if (receiptFileId != null) {
                try {
                    imageService.deleteImageFromS3(receiptFileId);
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
        // 그룹 정보 조회
        Group group = groupRepository.findById(dto.groupId()).orElseThrow(
                () -> new CustomException(ErrorCode.GROUP_NOT_FOUND)
        );

        // 그룹 멤버 조회
        List<GroupMember> groupMembers = groupMemberRepository.findByGroupId(dto.groupId());
        if (groupMembers.isEmpty()) {
            throw new CustomException(ErrorCode.GROUP_MEMBER_NOT_FOUND);
        }

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
            ExpenseSplit expenseSplit = ExpenseSplit.builder()
                    .expenseId(original.getExpenseId())
                    .userId(split.userId())
                    .amount(split.amount())
                    .build();
            expenseSplitRepository.save(expenseSplit);

            createShareExpense(original, split.userId(), split.amount());
        }
    }

    /**
     * 균등 분할 처리
     */
    private void processEqualSplit(Expense original, List<GroupMember> members) {
        BigDecimal totalAmount = original.getAmount();
        int memberCount = members.size();
        BigDecimal splitAmount = totalAmount.divide(BigDecimal.valueOf(memberCount), 2, RoundingMode.HALF_UP);

        for (GroupMember member : members) {
            // ExpenseSplit 생성
            ExpenseSplit split = ExpenseSplit.builder()
                    .expenseId(original.getExpenseId())
                    .userId(member.getUser().getUserId())
                    .amount(splitAmount)
                    .build();
            expenseSplitRepository.save(split);

            createShareExpense(original, member.getUser().getUserId(), splitAmount);
        }
    }

    /**
     * 개인 가계부에 분담 지출 생성
     */
    private void createShareExpense(Expense original, UUID userId, BigDecimal shareAmount) {
        Expense shareExpense = Expense.builder()
                .title(original.getTitle() + " (그룹 분담)")
                .amount(shareAmount)
                .category(original.getCategory())
                .expenseType(ExpenseType.SHARE)
                .userId(userId)
                .expenseDate(original.getExpenseDate())
                .build();

        shareExpense.setGroupId(original.getGroupId());
        shareExpense.updateMemo(original.getMemo());

        // 원본 지출 id 설정
        shareExpense.setOriginalExpenseId(original.getExpenseId());
        expenseRepository.save(shareExpense);
    }

    /**
     * 지출 정산 데이터 조회
     */
    private List<SplitDataDTO> getSplitData(Long expenseId) {
        return expenseSplitRepository.findByExpenseId(expenseId)
                .stream()
                .map(split -> new SplitDataDTO(split.getUserId(), split.getAmount()))
                .toList();
    }

    /**
     * 지출 조회
     */
    @Override
    public ExpenseResponseDTO getExpenseById(UUID userId, Long expenseId) {
        Expense expense = expenseRepository.findByExpenseIdWithPermission(expenseId, userId).orElseThrow(
                () -> new CustomException(ErrorCode.EXPENSE_NOT_FOUND)
        );

        // 그룹 지출 - 정산 데이터 포함
        List<SplitDataDTO> splitData = null;
        if (expense.getExpenseType() == ExpenseType.GROUP) {
            splitData = getSplitData(expenseId);
        }

        return ExpenseResponseDTO.from(expense, splitData);
    }

    /**
     * 지출 수정
     */
    @Override
    @Transactional
    public ExpenseResponseDTO updateExpense(UUID userId, Long expenseId, UpdateExpenseRequestDTO requestDTO, MultipartFile receiptFile) {
        Expense expense = expenseRepository.findByExpenseIdWithPermission(expenseId, userId).orElseThrow(
                () -> new CustomException(ErrorCode.EXPENSE_NOT_FOUND)
        );

        UUID newReceiptFileId = null;
        UUID oldReceiptFileId = expense.getReceiptImageFileId();

        try {
            expense.updateTitle(requestDTO.title());
            expense.updateAmount(requestDTO.amount());
            expense.updateCategory(requestDTO.category());
            expense.updateMemo(requestDTO.memo());
            expense.updateExpenseDate(requestDTO.date());

            // 영수증 파일 처리
            if (receiptFile != null && !receiptFile.isEmpty()) {
                // 새 영수증 업로드
                ImageFile uploadedImage = imageService.uploadImageToS3(receiptFile, "receipt-image", 10 * 1024 * 1024);
                newReceiptFileId = uploadedImage.getFileId();
                expense.addReceiptImage(uploadedImage.getFileId());

                // 기존 영수증 삭제 (새 파일 업로드 성공 후)
                if (oldReceiptFileId != null) {
                    imageService.deleteImageFromS3(oldReceiptFileId);
                }
            }

            // 그룹 지출의 경우 분담 정보 업데이트
            if (expense.getExpenseType() == ExpenseType.GROUP && expense.getSplitType() != null) {
                expense.setSplitType(requestDTO.splitType());
                updateGroupExpenseSplits(expense, requestDTO.splitData());
            }
        } catch (Exception e) {
            // 새로 업로드한 파일 정리 (업데이트 실패 시)
            if (newReceiptFileId != null) {
                try {
                    imageService.deleteImageFromS3(newReceiptFileId);
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
        log.info("updateGroupExpenseSplits: {}, {}", original.getSplitType(), newSplitData);
        switch (original.getSplitType()) {
            case EQUAL -> updateEqualSplit(original);
            case CUSTOM, SPECIFIC -> updateCustomSplit(original, newSplitData);
        }
    }

    /**
     * 커스텀 분할 업데이트
     */
    private void updateCustomSplit(Expense original, List<SplitDataDTO> newSplitData) {
        if (newSplitData == null || newSplitData.isEmpty()) {
            throw new CustomException(ErrorCode.SPLIT_DATA_REQUIRED);
        }

        List<ExpenseSplit> existingSplits = expenseSplitRepository.findByExpenseId(original.getExpenseId());
        Map<UUID, ExpenseSplit> existingSplitMap = existingSplits.stream()
                .collect(Collectors.toMap(ExpenseSplit::getUserId, split -> split));

        Set<UUID> newUserIds = newSplitData.stream()
                .map(SplitDataDTO::userId)
                .collect(Collectors.toSet());

        // 기존 사용자 중 제거된 사용자 처리
        existingSplits.stream()
                .filter(split -> !newUserIds.contains(split.getUserId()))
                .forEach(split -> {
                    expenseSplitRepository.delete(split);
                    deleteUserShareExpense(original.getExpenseId(), split.getUserId());
                });
        // 새로운 분할 데이터 처리
        newSplitData.forEach(splitData -> {
            ExpenseSplit existingSplit = existingSplitMap.get(splitData.userId());

            if (existingSplit != null) {
                // 기존 사용자
                existingSplit.updateAmount(splitData.amount());
                updateUserShareExpense(original, splitData.userId(), splitData.amount());
            } else {
                // 새로운 사용자
                ExpenseSplit newSplit = ExpenseSplit.builder()
                        .expenseId(original.getExpenseId())
                        .userId(splitData.userId())
                        .amount(splitData.amount())
                        .build();
                expenseSplitRepository.save(newSplit);

                createShareExpense(original, splitData.userId(), splitData.amount());
            }
        });
    }

    /**
     * 특정 사용자의 SHARE 삭제
     */
    private void deleteUserShareExpense(Long expenseId, UUID userId) {
        List<Expense> shareExpenses = expenseRepository.findByOriginalExpenseIdAndUserId(expenseId, userId);
        expenseRepository.deleteAll(shareExpenses);
    }

    /**
     * 균등 분할 업데이트
     */
    private void updateEqualSplit(Expense original) {
        // 기존 정보
        List<ExpenseSplit> existingSplits = expenseSplitRepository.findByExpenseId(original.getExpenseId());

        BigDecimal newAmount = original.getAmount().divide(BigDecimal.valueOf(existingSplits.size()), 2, RoundingMode.HALF_UP);

        // 변경이 필요한 것만 업데이트
        List<ExpenseSplit> toUpdate = existingSplits.stream()
                .filter(split -> !split.getAmount().equals(newAmount))
                .toList();

        if (!toUpdate.isEmpty()) {
            toUpdate.forEach(split -> {
                split.updateAmount(newAmount);
                updateUserShareExpense(original, split.getUserId(), newAmount);
            });
        }
    }

    /**
     * 특정 사용자의 SHARE 업데이트
     */
    private void updateUserShareExpense(Expense original, UUID userId, BigDecimal newAmount) {
        List<Expense> shareExpense = expenseRepository.findByOriginalExpenseIdAndUserId(original.getExpenseId(), userId);

        shareExpense.forEach(expense -> {
            expense.updateAmount(newAmount);
            expense.updateTitle(original.getTitle());
            expense.updateCategory(original.getCategory());
            expense.updateMemo(original.getMemo());
            expense.updateExpenseDate(original.getExpenseDate());
        });
    }

    /**
     * 지출 삭제
     */
    @Override
    @Transactional
    public void deleteExpense(UUID userId, Long expenseId) {
        Expense expense = expenseRepository.findByExpenseIdWithPermission(expenseId, userId).orElseThrow(
                () -> new CustomException(ErrorCode.EXPENSE_NOT_FOUND)
        );

        // 그룹 지출인 경우 연관 데이터 정리
        if (expense.getExpenseType() == ExpenseType.GROUP) {
            expenseSplitRepository.deleteByExpenseId(expenseId);
            expenseRepository.deleteByOriginalExpenseId(expenseId);
        }

        expenseRepository.deleteById(expenseId);
    }

    /**
     * 그룹 분담금 조회
     */
    @Override
    public List<GroupShareExpenseResponseDTO> getGroupShareExpenses(UUID userId, int year, int month) {
        log.info("=== getGroupShareExpenses 호출 ===");
        log.info("userId: {}, year: {}, month: {}", userId, year, month);

        // 1. SHARE 타입 지출들 조회
        List<Expense> shareExpenses = expenseRepository.findShareExpensesByUserAndMonth(userId, year, month);
        log.info("조회된 SHARE 지출 개수: {}", shareExpenses.size());


        return shareExpenses.stream()
                .map(shareExpense -> {
                    log.debug("--- SHARE 지출 처리 중 ---");
                    log.debug("SHARE 지출 ID: {}", shareExpense.getExpenseId());
                    log.debug("SHARE 지출 제목: {}", shareExpense.getTitle());
                    log.debug("SHARE 분담 금액: {}", shareExpense.getAmount());
                    log.debug("원본 지출 ID: {}", shareExpense.getOriginalExpenseId());

                    // 2. 원본 그룹 지출 조회 (더 많은 정보를 위해)
                    Expense originalExpense = null;
                    if (shareExpense.getOriginalExpenseId() != null) {
                        originalExpense = expenseRepository.findById(shareExpense.getOriginalExpenseId())
                                .orElse(null);
                    }

                    // 3. 분할 정보 조회 (원본 지출 기준)
                    List<SplitDataDTO> splitData = null;
                    if (originalExpense != null) {
                        List<ExpenseSplit> splits = expenseSplitRepository.findByExpenseId(originalExpense.getExpenseId());
                        splitData = splits.stream()
                                .map(split -> new SplitDataDTO(split.getUserId(), split.getAmount()))
                                .toList();
                    }

                    // 4. 그룹 이름 조회
                    String groupName = shareExpense.getGroup() != null ?
                            shareExpense.getGroup().getGroupName() : "";
                    log.debug("그룹 이름: {}", groupName);


                    GroupShareExpenseResponseDTO result = GroupShareExpenseResponseDTO.from(
                            shareExpense,
                            originalExpense,
                            groupName,
                            splitData
                    );
                    log.debug("생성된 응답 데이터 - expenseId: {}, title: {}, myShareAmount: {}",
                            result.expenseId(), result.title(), result.myShareAmount());

                    return result;

                })
                .toList();
    }

    /**
     * 영수증 조회
     */
    @Override
    public String getReceiptImageUrl(Long expenseId, UUID userId) {
        // 권한 체크 포함해서 지출 조회
        Expense expense = expenseRepository.findByExpenseIdWithPermission(expenseId, userId).orElseThrow(
                () -> new CustomException(ErrorCode.EXPENSE_NOT_FOUND)
        );

        // 영수증 파일 없으면 null 반환
        if (expense.getReceiptImageFileId() == null) {
            return null;
        }

        return imageService.generatePresignedUrl(expense.getReceiptImageFileId());
    }

    /**
     * 영수증만 삭제 (지출은 유지)
     */
    @Override
    @Transactional
    public void deleteReceiptImage(Long expenseId, UUID userId) {
        Expense expense = expenseRepository.findByExpenseIdWithPermission(expenseId, userId).orElseThrow(
                () -> new CustomException(ErrorCode.EXPENSE_NOT_FOUND)
        );

        // 영수증이 없으면 예외
        if (expense.getReceiptImageFileId() == null) {
            throw new CustomException(ErrorCode.FILE_NOT_FOUND);
        }

        // S3에서 파일 삭제
        imageService.deleteImageFromS3(expense.getReceiptImageFileId());

        expense.addReceiptImage(null);
    }

    /**
     * 개인 지출 월별 추이 조회
     */
    @Override
    public MonthlyExpenseTrendResponseDTO getPersonalExpenseTrend(UUID userId, int year, int month, int months, String category) {
        log.info("개인 지출 추이 조회: userId={}, year={}, month={}, months={}, category={}", userId, year, month, months, category);

        // 날짜 범위 계산
        DateRange dateRange = calculateDateRange(year, month, months);

        ExpenseCategory categoryEnum = category != null ? ExpenseCategory.valueOf(category.toUpperCase()) : null;
        // Repository 조회
        List<Expense> expenses = expenseRepository.findPersonalExpensesByMonthRange(userId, dateRange.startDate(), dateRange.endDate(), categoryEnum);

        // 월별 그룹핑 및 응답
        return createTrendResponse(expenses, dateRange);
    }

    /**
     * 그룹 지출 월별 추이 조회
     */
    @Override
    public MonthlyExpenseTrendResponseDTO getGroupExpenseTrend(UUID userId, UUID groupId, int year, int month, int months, String category) {
        // 그룹 멤버인지 확인
        boolean isMember = groupMemberRepository.existsByGroupIdAndUserId(groupId, userId);
        if (!isMember) {
            throw new CustomException(ErrorCode.GROUP_MEMBER_NOT_FOUND);
        }

        log.info("그룹 지출 추이 조회: groupId={}, year={}, month={}, months={}, category={}",
                groupId, year, month, months, category);

        // 날짜 범위 계산
        DateRange dateRange = calculateDateRange(year, month, months);

        ExpenseCategory categoryEnum = category != null ? ExpenseCategory.valueOf(category.toUpperCase()) : null;
        // Repository에서 데이터 조회
        List<Expense> expenses = expenseRepository.findGroupExpensesByMonthRange(groupId, dateRange.startDate(), dateRange.endDate(), categoryEnum);

        // 월별 그룹핑 및 응답 생성
        return createTrendResponse(expenses, dateRange);
    }


    /**
     * 날짜 범위 계산
     */
    private DateRange calculateDateRange(int year, int month, int months) {
        LocalDate endDate = LocalDate.of(year, month, 1).with(TemporalAdjusters.lastDayOfMonth());
        LocalDate startDate = endDate.minusMonths(months - 1).with(TemporalAdjusters.firstDayOfMonth());

        return new DateRange(startDate, endDate);
    }

    /**
     * 월별 추이 응답 생성
     */
    private MonthlyExpenseTrendResponseDTO createTrendResponse(List<Expense> expenses, DateRange dateRange) {
        Map<String, List<ExpenseResponseDTO>> monthlyData = createMonthlyGrouping(expenses, dateRange);
        return MonthlyExpenseTrendResponseDTO.create(monthlyData, dateRange.startDate, dateRange.endDate);
    }

    /**
     * 지출 데이터를 월별로 그룹핑
     */
    private Map<String, List<ExpenseResponseDTO>> createMonthlyGrouping(List<Expense> expenses, DateRange dateRange) {
        // 지출 데이터 월별로 그룹핑
        Map<String, List<ExpenseResponseDTO>> monthlyData = expenses.stream()
                .collect(Collectors.groupingBy(
                        expense -> expense.getExpenseDate().format(DateTimeFormatter.ofPattern("yyyy-MM")),
                        LinkedHashMap::new,  // 순서 보장
                        Collectors.mapping(
                                expense -> convertToResponseDTO(expense),
                                Collectors.toList()
                        )
                ));

        // 데이터 없는 달 빈 배열로 추가
        fillEmptyMonths(monthlyData, dateRange.startDate, dateRange.endDate);
        return sortByMonthKey(monthlyData);
    }

    /**
     * Expense를 ExpenseResponseDTO로 변환
     */
    private ExpenseResponseDTO convertToResponseDTO(Expense expense) {
        // 그룹의 경우 분할 데이터 포함
        List<SplitDataDTO> splitData = null;
        if (expense.getExpenseType() == ExpenseType.GROUP) {
            splitData = getSplitData(expense.getExpenseId());
        }
        return ExpenseResponseDTO.from(expense, splitData);
    }

    /**
     * 데이터 업는 달에 빈 배열 추가
     */
    private void fillEmptyMonths(Map<String, List<ExpenseResponseDTO>> monthlyData, LocalDate startDate, LocalDate endDate) {
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            String monthKey = current.format(DateTimeFormatter.ofPattern("yyyy-MM"));
            monthlyData.putIfAbsent(monthKey, new ArrayList<>());
            current = current.plusMonths(1);
        }
    }

    /**
     * 월별 키로 정렬
     */
    private Map<String, List<ExpenseResponseDTO>> sortByMonthKey(Map<String, List<ExpenseResponseDTO>> monthlyData) {
        return monthlyData.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(LinkedHashMap::new, (m, e) -> m.put(e.getKey(), e.getValue()),
                        LinkedHashMap::putAll);
    }

    private record DateRange(LocalDate startDate, LocalDate endDate) {
    }

    /**
     * LLM 지출 분석 관련 메서드
     */
    @Override
    public PageWithSummaryResponse<GroupShareExpenseResponseDTO> getGroupShareExpensesWithPaging(
            UUID userId, UUID groupId, int year, int month, Pageable pageable
    ) {
        // 1. 기존 페이징 로직 (그대로 유지)
        Page<Expense> shareExpenses = expenseRepository.findShareExpensesByGroupAndMonthWithPaging(
                userId, groupId, year, month, pageable
        );

        Page<GroupShareExpenseResponseDTO> shareExpenseResponsePage = shareExpenses.map(expense -> {
            // 원본 그룹 지출 조회
            Expense originalExpense = null;
            if (expense.getOriginalExpenseId() != null) {
                originalExpense = expenseRepository.findById(expense.getOriginalExpenseId())
                        .orElse(null);
            }

            // 분할 정보 조회
            List<SplitDataDTO> splitData = null;
            if (originalExpense != null) {
                List<ExpenseSplit> splits = expenseSplitRepository.findByExpenseId(originalExpense.getExpenseId());
                splitData = splits.stream()
                        .map(split -> new SplitDataDTO(split.getUserId(), split.getAmount()))
                        .toList();
            }
            // 그룹 이름 조회
            String groupName = expense.getGroup() != null ? expense.getGroup().getGroupName() : "";

            return GroupShareExpenseResponseDTO.from(
                    expense, originalExpense, groupName, splitData
            );
        });

        // 2. 통계 계산을 위한 전체 데이터 조회 (새로 추가)
        if (!shareExpenseResponsePage.hasContent()) {
            return PageWithSummaryResponse.of(shareExpenseResponsePage, ExpenseSummary.empty());
        }

        // 전체 데이터 조회 (페이징 없이)
        Page<Expense> allShareExpenses = expenseRepository.findShareExpensesByGroupAndMonthWithPaging(
                userId, groupId, year, month, Pageable.unpaged()
        );

        List<GroupShareExpenseResponseDTO> allShareExpensesDTOs = allShareExpenses.getContent().stream()
                .map(expense -> {
                    // 동일한 변환 로직
                    Expense originalExpense = null;
                    if (expense.getOriginalExpenseId() != null) {
                        originalExpense = expenseRepository.findById(expense.getOriginalExpenseId())
                                .orElse(null);
                    }

                    List<SplitDataDTO> splitData = null;
                    if (originalExpense != null) {
                        List<ExpenseSplit> splits = expenseSplitRepository.findByExpenseId(originalExpense.getExpenseId());
                        splitData = splits.stream()
                                .map(split -> new SplitDataDTO(split.getUserId(), split.getAmount()))
                                .toList();
                    }
                    String groupName = expense.getGroup() != null ? expense.getGroup().getGroupName() : "";

                    return GroupShareExpenseResponseDTO.from(
                            expense, originalExpense, groupName, splitData
                    );
                })
                .collect(Collectors.toList());

        // 3. 통계 정보 계산
        ExpenseSummary summary = calculateShareExpensesSummary(allShareExpensesDTOs);

        return PageWithSummaryResponse.of(shareExpenseResponsePage, summary);
    }

    /**
     * GroupShareExpenseResponseDTO 리스트로 분담 지출 통계 정보 계산
     */
    private ExpenseSummary calculateShareExpensesSummary(List<GroupShareExpenseResponseDTO> expenses) {
        if (expenses.isEmpty()) {
            return ExpenseSummary.empty();
        }

        // 전체 지출 금액 (원본 지출 기준)
        BigDecimal totalAmount = expenses.stream()
                .map(GroupShareExpenseResponseDTO::amount)  // 원본 지출 총액
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 내 분담금 총합 (새로 추가)
        BigDecimal myTotalShareAmount = expenses.stream()
                .map(GroupShareExpenseResponseDTO::myShareAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal averageAmount = totalAmount.divide(
                BigDecimal.valueOf(expenses.size()), 2, RoundingMode.HALF_UP
        );

        BigDecimal maxAmount = expenses.stream()
                .map(GroupShareExpenseResponseDTO::amount)  // 원본 지출 기준 최대값
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        long totalCount = expenses.size();
        long myShareCount = expenses.size();  // 내 분담금 건수 (현재는 동일)

        // 카테고리별 통계 계산 (내 분담금 기준)
        Map<String, CategoryStats> categoryStats = calculateCategoryStats(
                expenses.stream().map(e -> new ExpenseStatItem(e.category().name(), e.myShareAmount())).toList(),
                myTotalShareAmount  // 내 분담금 총합 기준으로 변경
        );

        return new ExpenseSummary(
                totalAmount,
                averageAmount,
                maxAmount,
                totalCount,
                myTotalShareAmount,
                myShareCount,
                categoryStats
        );
    }

    @Override
    public PageWithSummaryResponse<CombinedExpenseResponseDTO> getCombinedExpenses(
            UUID userId, int year, int month, String category, String search, Pageable pageable
    ) {
        ExpenseCategory categoryEnum = parseCategory(category);

        // 1. 개인 지출과 분담 지출 DTO 리스트를 각각 생성
        List<CombinedExpenseResponseDTO> personalExpenseDTOs = fetchPersonalExpenseDTOs(userId, year, month, categoryEnum, search);
        List<CombinedExpenseResponseDTO> shareExpenseDTOs = fetchShareExpenseDTOs(userId, year, month, categoryEnum, search);

        // 2. 두 리스트를 통합하고 정렬
        List<CombinedExpenseResponseDTO> allExpenses = new ArrayList<>();
        allExpenses.addAll(personalExpenseDTOs);
        allExpenses.addAll(shareExpenseDTOs);
        allExpenses.sort(Comparator.comparing(CombinedExpenseResponseDTO::date).reversed());

        // 3. 통계 정보 계산
        ExpenseSummary summary = calculateExpenseSummary(allExpenses);

        // 4. 수동 페이징 처리
        Page<CombinedExpenseResponseDTO> page = applyManualPaging(allExpenses, pageable);

        return PageWithSummaryResponse.of(page, summary);
    }

    /**
     * 사용자의 개인 지출 내역을 조회하여 DTO 리스트 변환
     */
    private List<CombinedExpenseResponseDTO> fetchPersonalExpenseDTOs(UUID userId, int year, int month, ExpenseCategory category, String search) {
        List<Expense> personalExpenses = expenseRepository.findPersonalExpensesForCombined(
                userId, year, month, category, search, Pageable.unpaged()
        ).getContent();

        return personalExpenses.stream()
                .map(CombinedExpenseResponseDTO::fromPersonal)
                .collect(Collectors.toList());
    }

    /**
     * 사용자의 그룹 분담 지출 내역을 조회하여 DTO 리스트 변환 (N+1 해결)
     */
    private List<CombinedExpenseResponseDTO> fetchShareExpenseDTOs(UUID userId, int year, int month, ExpenseCategory category, String search) {
        List<Expense> shareExpenses = expenseRepository.findShareExpensesForCombined(
                userId, year, month, category, search, Pageable.unpaged()
        ).getContent();

        if (shareExpenses.isEmpty()) {
            return Collections.emptyList();
        }

        // N+1 문제 해결을 위한 데이터 일괄 조회
        Map<Long, Expense> originalExpenseMap = findOriginalExpenses(shareExpenses);
        Map<UUID, String> groupNameMap = findGroupNames(shareExpenses);

        // DTO 변환
        return shareExpenses.stream()
                .map(shareExpense -> {
                    Expense originalExpense = originalExpenseMap.get(shareExpense.getOriginalExpenseId());
                    String groupName = groupNameMap.get(shareExpense.getGroupId());
                    return CombinedExpenseResponseDTO.fromGroupShare(shareExpense, originalExpense, groupName);
                })
                .collect(Collectors.toList());
    }

    /**
     * 분담 지출 목록에 대한 원본 지출 정보들 일괄 조회
     */
    private Map<Long, Expense> findOriginalExpenses(List<Expense> shareExpenses) {
        Set<Long> originalExpenseIds = shareExpenses.stream()
                .map(Expense::getOriginalExpenseId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (originalExpenseIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return expenseRepository.findAllById(originalExpenseIds).stream()
                .collect(Collectors.toMap(Expense::getExpenseId, expense -> expense));
    }

    /**
     * 분담 지출 목록에 대한 그룹 이름 정보들 일괄 조회
     */
    private Map<UUID, String> findGroupNames(List<Expense> shareExpenses) {
        Set<UUID> groupIds = shareExpenses.stream()
                .map(Expense::getGroupId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (groupIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return groupRepository.findAllById(groupIds).stream()
                .collect(Collectors.toMap(Group::getGroupId, Group::getGroupName));
    }

    /**
     * 리스트에 대해 수동으로 페이징 처리 적용
     */
    private <T> Page<T> applyManualPaging(List<T> sourceList, Pageable pageable) {
        int start = (int) pageable.getOffset();
        if (start > sourceList.size()) {
            return new PageImpl<>(Collections.emptyList(), pageable, sourceList.size());
        }
        int end = Math.min(start + pageable.getPageSize(), sourceList.size());
        List<T> pageContent = sourceList.subList(start, end);
        return new PageImpl<>(pageContent, pageable, sourceList.size());
    }

    /**
     * 사용자가 해당 그룹의 멤버인지 검증
     */
    private void validateGroupMembership(UUID userId, UUID groupId) {
        if (!groupMemberRepository.existsByGroupIdAndUserId(groupId, userId)) {
            throw new CustomException(ErrorCode.GROUP_MEMBER_NOT_FOUND);
        }
    }

    /**
     * 카테고리 문자열을 ExpenseCategory Enum 변환
     */
    private ExpenseCategory parseCategory(String category) {
        if (category == null || category.trim().isEmpty()) {
            return null;
        }
        try {
            return ExpenseCategory.valueOf(category.toUpperCase());
        } catch (IllegalArgumentException e) {
            // 또는 로깅 후 null 반환 등 정책에 맞게 처리
            throw new CustomException(ErrorCode.INVALID_CATEGORY);
        }
    }

    /**
     * 조건에 따라 그룹 지출 내역을 데이터베이스에서 페이징하여 조회
     */
    private Page<Expense> fetchGroupExpenses(UUID groupId, int year, int month,
                                             ExpenseCategory category, String search, Pageable pageable) {
        if (search != null && !search.trim().isEmpty()) {
            return expenseRepository.findGroupExpensesBySearchAndMonthWithPaging(
                    groupId, search.trim(), year, month, pageable
            );
        } else if (category != null) {
            return expenseRepository.findGroupExpensesByCategoryAndMonthWithPaging(
                    groupId, category, year, month, pageable
            );
        } else {
            return expenseRepository.findGroupExpensesByMonthWithPaging(
                    groupId, year, month, pageable
            );
        }
    }

    /**
     * CombinedExpenseResponseDTO 리스트로 통계 정보 계산
     */
    private ExpenseSummary calculateExpenseSummary(List<CombinedExpenseResponseDTO> expenses) {
        if (expenses.isEmpty()) {
            return ExpenseSummary.empty();
        }

        // 기본 통계 계산
        BigDecimal totalAmount = expenses.stream()
                .map(CombinedExpenseResponseDTO::myShareAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal averageAmount = totalAmount.divide(
                BigDecimal.valueOf(expenses.size()), 2, RoundingMode.HALF_UP
        );

        BigDecimal maxAmount = expenses.stream()
                .map(CombinedExpenseResponseDTO::myShareAmount)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        long totalCount = expenses.size();

        // 개인/통합 지출의 경우 totalAmount와 myTotalShareAmount가 동일
        BigDecimal myTotalShareAmount = totalAmount;
        long myShareCount = totalCount;

        // 카테고리별 통계 계산
        Map<String, CategoryStats> categoryStats = calculateCategoryStats(
                expenses.stream().map(e -> new ExpenseStatItem(e.category().name(), e.myShareAmount())).toList(),
                totalAmount
        );

        return new ExpenseSummary(
                totalAmount,
                averageAmount,
                maxAmount,
                totalCount,
                myTotalShareAmount,
                myShareCount,
                categoryStats
        );
    }

    /**
     * 공통 카테고리별 통계 계산
     */
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

    /**
     * 통계 계산용 내부 record
     */
    private record ExpenseStatItem(String category, BigDecimal amount) {
    }

    @Override
    public PageWithSummaryResponse<ExpenseResponseDTO> getGroupExpensesWithStats(
            UUID userId, UUID groupId, int year, int month,
            String category, String search, Pageable pageable
    ) {
        // 1. 사전 조건 검증 (기존 메서드 활용)
        validateGroupMembership(userId, groupId);
        ExpenseCategory categoryEnum = parseCategory(category);

        // 2. 페이징된 데이터 조회 (기존 fetchGroupExpenses 메서드 활용)
        Page<Expense> groupExpensesPage = fetchGroupExpenses(groupId, year, month, categoryEnum, search, pageable);

        if (!groupExpensesPage.hasContent()) {
            return PageWithSummaryResponse.of(Page.<ExpenseResponseDTO>empty(pageable), ExpenseSummary.empty());
        }

        // 3. Page<Expense>를 Page<ExpenseResponseDTO>로 변환
        Page<ExpenseResponseDTO> expenseResponsePage = groupExpensesPage.map(expense -> {
            List<SplitDataDTO> splitData = getSplitData(expense.getExpenseId());
            return ExpenseResponseDTO.from(expense, splitData);
        });

        // 4. 통계 계산을 위한 전체 데이터 조회 (기존 fetchGroupExpenses 활용)
        Page<Expense> allExpensesPage = fetchGroupExpenses(groupId, year, month, categoryEnum, search, Pageable.unpaged());

        List<ExpenseResponseDTO> allExpensesForStats = allExpensesPage.getContent().stream()
                .map(expense -> {
                    List<SplitDataDTO> splitData = getSplitData(expense.getExpenseId());
                    return ExpenseResponseDTO.from(expense, splitData);
                })
                .collect(Collectors.toList());

        // 5. 통계 정보 계산
        ExpenseSummary summary = calculateGroupExpensesSummary(allExpensesForStats);

        return PageWithSummaryResponse.of(expenseResponsePage, summary);
    }

    /**
     * ExpenseResponseDTO 리스트로 그룹 지출 통계 정보 계산
     */
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

        BigDecimal maxAmount = expenses.stream()
                .map(ExpenseResponseDTO::amount)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        long totalCount = expenses.size();

        // 그룹 지출의 경우 totalAmount와 myTotalShareAmount가 동일
        // (내가 등록한 그룹 지출이므로)
        BigDecimal myTotalShareAmount = totalAmount;
        long myShareCount = totalCount;

        // 기존 calculateCategoryStats 메서드 활용
        Map<String, CategoryStats> categoryStats = calculateCategoryStats(
                expenses.stream().map(e -> new ExpenseStatItem(e.category().name(), e.amount())).toList(),
                totalAmount
        );

        return new ExpenseSummary(
                totalAmount,
                averageAmount,
                maxAmount,
                totalCount,
                myTotalShareAmount,
                myShareCount,
                categoryStats
        );
    }

    /**
     * LLM 지출 분석
     * @param userId 사용자의 ID
     * @param requestDTO 시작일, 종료일
     * @return 4가지 유형 - 예산 사용률, 일 평균 지출(월말 예상), 분석 결과, 카테고리별 상세 분석
     */
    @Override
    public AnalyzeExpenseResponseDTO analyzeExpenses(UUID userId, AnalyzeExpenseRequestDTO requestDTO) {
        List<Expense> expenses = expenseRepository.findPersonalAndShareExpensesByDateRange(userId, requestDTO.startDate(), requestDTO.endDate());
        BigDecimal totalBudget = getUserBudget(userId);

        if (expenses.isEmpty()) {
            return createEmptyAnalysis(totalBudget);
        }

        BigDecimal totalSpending = calculateTotalSpending(expenses);

        AnalyzeExpenseResponseDTO.BudgetUsage budgetUsage = calculateBudgetUsage(totalSpending, totalBudget);
        AnalyzeExpenseResponseDTO.DailySpending dailySpending = calculateDailySpending(totalSpending,requestDTO.startDate(),requestDTO.endDate());

        List<AnalyzeExpenseResponseDTO.CategoryDetail> categoryDetails = calculateCategoryDetails(expenses,totalSpending);

        AnalyzeExpenseResponseDTO.Suggestion suggestion = getLlmAnalysisResult(expenses);

        String mainFinding = createMainFinding(categoryDetails);

        AnalyzeExpenseResponseDTO.AnalysisResult analysisResult = new AnalyzeExpenseResponseDTO.AnalysisResult(mainFinding, suggestion);

        return new AnalyzeExpenseResponseDTO(budgetUsage,dailySpending,analysisResult,categoryDetails);
    }

    /**
     * LLM 지출 분석 기록 조회
     */
    @Override
    public List<ExpenseAnalysisHistoryDTO> getExpenseAnalysisHistory(UUID userId) {

        User user = userRepository.findById(userId).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        List<ExpenseAnalysisHistory> historyList = expenseAnalysisHistoryRepository.findByUserOrderByCreatedAtDesc(user);

        return historyList.stream()
                .map(ExpenseAnalysisHistoryDTO::from)
                .collect(Collectors.toList());

    }

    /**
     * LLM 지출 분석 내역 저장
     */
    @Override
    @Transactional
    public void saveExpenseAnalysisHistory(UUID userId, AnalyzeExpenseRequestDTO requestDTO, AnalyzeExpenseResponseDTO analysisResponseDTO) {
        User user = userRepository.findById(userId).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        ExpenseAnalysisHistory history = ExpenseAnalysisHistory.builder()
                .user(user)
                .startDate(requestDTO.startDate())
                .endDate(requestDTO.endDate())
                .budgetUsagePercentage(analysisResponseDTO.budgetUsage().percentage())
                .budgetUsageCurrentSpending(analysisResponseDTO.budgetUsage().currentSpending())
                .budgetUsageTotalBudget(analysisResponseDTO.budgetUsage().totalBudget())
                .dailySpendingAverageSoFar(analysisResponseDTO.dailySpending().averageSoFar())
                .dailySpendingEstimatedEom(analysisResponseDTO.dailySpending().estimatedEom())
                .mainFinding(analysisResponseDTO.analysisResult().mainFinding())
                .suggestionTitle(analysisResponseDTO.analysisResult().suggestion().title())
                .suggestionDescription(analysisResponseDTO.analysisResult().suggestion().description())
                .suggestionEffect(analysisResponseDTO.analysisResult().suggestion().effect())
                .suggestionDifficulty(analysisResponseDTO.analysisResult().suggestion().difficulty())
                .build();

        expenseAnalysisHistoryRepository.save(history);
    }

    /**
     * 사용자의 예산 정보를 조회합니다.
     * @param userId 사용자의 UUID
     * @return 사용자의 예산
     */
    private BigDecimal getUserBudget(UUID userId){
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
    private AnalyzeExpenseResponseDTO.BudgetUsage calculateBudgetUsage(BigDecimal totalSpending, BigDecimal totalBudget){
        if (totalBudget.compareTo(BigDecimal.ZERO) == 0) {
            return new AnalyzeExpenseResponseDTO.BudgetUsage(0.0, totalSpending, totalBudget);
        }
        BigDecimal percentage = totalSpending.multiply(BigDecimal.valueOf(100)).divide(totalBudget,2,RoundingMode.HALF_UP);
        return new AnalyzeExpenseResponseDTO.BudgetUsage(percentage.doubleValue(), totalSpending, totalBudget);
    }

    /**
     * 일평균, 월말 지출 객체 생성
     */
    private AnalyzeExpenseResponseDTO.DailySpending calculateDailySpending(BigDecimal totalSpending, LocalDate startDate, LocalDate endDate){
        long days = java.time.temporal.ChronoUnit.DAYS.between(startDate, LocalDate.now()) + 1;
        BigDecimal averageSoFar = totalSpending.divide(BigDecimal.valueOf(days),0,RoundingMode.HALF_UP);
        int daysInMonth = endDate.lengthOfMonth();
        BigDecimal estimatedEom = averageSoFar.multiply(BigDecimal.valueOf(daysInMonth)); // 월말 예상 지출

        return new AnalyzeExpenseResponseDTO.DailySpending(averageSoFar,estimatedEom);
    }

    /**
     * 카테고리별 상세 분석 객체 리스트 생성
     */
    private List<AnalyzeExpenseResponseDTO.CategoryDetail> calculateCategoryDetails(List<Expense> expenses, BigDecimal totalSpending){
        if(totalSpending.compareTo(BigDecimal.ZERO) == 0){
            return Collections.emptyList();
        }

        Map<ExpenseCategory,List<Expense>> expensesByCategory = expenses.stream()
                .collect(Collectors.groupingBy(Expense::getCategory));

        return expensesByCategory.entrySet().stream()
                .map(entry -> {
                    ExpenseCategory category = entry.getKey();
                    List<Expense> categoryExpenses = entry.getValue();
                    BigDecimal categoryTotal = calculateTotalSpending(categoryExpenses);
                    double percentage = categoryTotal.divide(totalSpending,4,RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100)).doubleValue();
                    int count = categoryExpenses.size();
                    return new AnalyzeExpenseResponseDTO.CategoryDetail(category.name(),percentage,categoryTotal,count);
                }).sorted(Comparator.comparing(AnalyzeExpenseResponseDTO.CategoryDetail::percentage).reversed()).toList();
    }

    /**
     * LLM으로부터 개선 제안을 받아옴
     */
    private AnalyzeExpenseResponseDTO.Suggestion getLlmAnalysisResult(List<Expense> expenses){
        List<ExpenseResponseDTO> expenseDTOs = expenses.stream()
                .map(this::convertToResponseDTO)
                .toList();
        try{
            String expenseJson = objectMapper.writeValueAsString(expenseDTOs);
            return expenseAnalyzer.analyzerExpenseData(expenseJson);
        } catch (JsonProcessingException e){
            log.error("DTO to JSON 변환 실패");
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
                new AnalyzeExpenseResponseDTO.Suggestion("지출 내역을 추가하고 다시 시도해주세요.","없음","없음", "없음"));

        return new AnalyzeExpenseResponseDTO(budgetUsage,dailySpending,analysisResult,Collections.emptyList());
    }
    /**
     * 가장 지출이 큰 카테고리를 찾아 분석요약(mainFinding) 생성
     */
    private String createMainFinding(List<AnalyzeExpenseResponseDTO.CategoryDetail> categoryDetails) {
        if (categoryDetails == null || categoryDetails.isEmpty()){
            return "주요 지출 항목이 없습니다.";
        }
        AnalyzeExpenseResponseDTO.CategoryDetail maxCategoryDetail = categoryDetails.get(0);

        return String.format("%s 지출 집중", maxCategoryDetail.category());
    }

}



