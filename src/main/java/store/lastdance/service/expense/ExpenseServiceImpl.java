package store.lastdance.service.expense;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import store.lastdance.domain.common.ImageFile;
import store.lastdance.domain.expense.Expense;
import store.lastdance.domain.expense.ExpenseCategory;
import store.lastdance.domain.expense.ExpenseSplit;
import store.lastdance.domain.expense.ExpenseType;
import store.lastdance.domain.group.Group;
import store.lastdance.domain.group.GroupMember;
import store.lastdance.dto.expense.*;
import store.lastdance.exception.CustomException;
import store.lastdance.exception.ErrorCode;
import store.lastdance.repository.expense.ExpenseRepository;
import store.lastdance.repository.expense.ExpenseSplitRepository;
import store.lastdance.repository.group.GroupMemberRepository;
import store.lastdance.repository.group.GroupRepository;
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
    private final ImageService imageService;

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
     * 개인 지출 조회
     */
    @Override
    public List<ExpenseResponseDTO> getPersonalExpenses(UUID userId, int year, int month, String category, String search) {
        List<Expense> expenses;

        // 카테고리 변환
        ExpenseCategory categoryEnum = category != null ? ExpenseCategory.valueOf(category.toUpperCase()) : null;

        if (search != null && !search.trim().isEmpty()) {
            // 검색어가 있는 경우
            expenses = expenseRepository.findPersonalExpensesBySearch(userId, search.trim(), year, month);
        } else if (categoryEnum != null) {
            // 카테고리 필터가 있는 경우
            expenses = expenseRepository.findPersonalExpensesByCategoryAndMonth(userId, categoryEnum, year, month);
        } else {
            // 기본 조회 (PERSONAL + SHARE)
            expenses = expenseRepository.findPersonalExpensesByMonth(userId, year, month);
        }

        return expenses.stream()
                .map(expense -> {
                    // GROUP 타입 지출의 경우 분할 데이터 포함
                    List<SplitDataDTO> splitData = null;
                    if (expense.getExpenseType() == ExpenseType.GROUP) {
                        splitData = getSplitData(expense.getExpenseId());
                    }
                    return ExpenseResponseDTO.from(expense, splitData);
                })
                .toList();
    }

    /**
     * 그룹 지출 조회
     */
    @Override
    public List<ExpenseResponseDTO> getGroupExpenses(UUID userId, UUID groupId, int year, int month) {
        // 해당 그룹 멤버인지 확인
        boolean isMember = groupMemberRepository.existsByGroupIdAndUserId(groupId, userId);
        if (!isMember) {
            throw new CustomException(ErrorCode.GROUP_MEMBER_NOT_FOUND);
        }

        // 그룹 지출 조회 (GROUP)
        List<Expense> expenses = expenseRepository.findGroupExpensesByMonth(groupId, year, month);

        return expenses.stream()
                .map(expense -> {
                    // 분할 데이터 포함
                    List<SplitDataDTO> splitData = getSplitData(expense.getExpenseId());
                    return ExpenseResponseDTO.from(expense, splitData);
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
        while(!current.isAfter(endDate)) {
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
}
