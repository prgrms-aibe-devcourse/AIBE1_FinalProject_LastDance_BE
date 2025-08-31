package store.lastdance.service.expense;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import store.lastdance.converter.expense.ExpenseConverter;
import store.lastdance.domain.common.ImageFile;
import store.lastdance.domain.expense.Expense;
import store.lastdance.domain.expense.ExpenseCategory;
import store.lastdance.domain.expense.ExpenseSplit;
import store.lastdance.domain.expense.ExpenseType;
import store.lastdance.domain.group.Group;
import store.lastdance.domain.group.GroupMember;
import store.lastdance.domain.user.User;
import store.lastdance.dto.expense.*;
import store.lastdance.dto.response.PageWithSummaryResponse;
import store.lastdance.exception.CustomException;
import store.lastdance.exception.ErrorCode;
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

    /**
     * 개인 지출 등록
     */
    @Override
    @Transactional
    public ExpenseResponseDTO createPersonalExpense(UUID userId, CreatePersonalExpenseRequestDTO requestDTO, MultipartFile receiptFile) {
        Expense expense = createBaseExpense(userId, requestDTO, ExpenseType.PERSONAL, receiptFile);

        Expense savedExpense = expenseRepository.save(expense);
//        return ExpenseResponseDTO.from(savedExpense);
        return expenseConverter.toResponseDTO(savedExpense);
    }

    /**
     * 그룹 지출 등록
     */
    @Override
    @Transactional
    public ExpenseResponseDTO createGroupExpense(UUID userId, CreateGroupExpenseRequestDTO requestDTO, MultipartFile receiptFile) {
        Expense expense = createBaseExpense(userId, requestDTO, ExpenseType.GROUP, receiptFile);

        // 그룹 정보 설정
        Group group = findGroupById(requestDTO.groupId());
        expense.updateGroup(group);
        expense.updateSplitType(requestDTO.splitType());

        Expense savedExpense = expenseRepository.save(expense);

        // 그룹 지출 정산 처리
        processGroupExpenseSplit(savedExpense, requestDTO);

//        return ExpenseResponseDTO.from(savedExpense);
        return expenseConverter.toResponseDTO(savedExpense);
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
     * 지출 조회
     */
    @Override
    public ExpenseResponseDTO getExpenseById(UUID userId, Long expenseId) {
        User user = findUserById(userId);

        Expense expense = expenseRepository.findByExpenseIdWithPermission(expenseId, user).orElseThrow(
                () -> new CustomException(ErrorCode.EXPENSE_NOT_FOUND)
        );

        // 그룹 지출 - 정산 데이터 포함
        List<SplitDataDTO> splitData = null;
        if (expense.getExpenseType() == ExpenseType.GROUP) {
            splitData = getSplitData(expense);
        }

//        return ExpenseResponseDTO.from(expense, splitData);
        return expenseConverter.toResponseDTO(expense, splitData);
    }

    /**
     * 지출 수정
     */
    @Override
    @Transactional
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

//        return ExpenseResponseDTO.from(expense);
        return expenseConverter.toResponseDTO(expense);
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
    @Transactional
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
     * 그룹 분담금 조회
     */
    @Override
    public List<GroupShareExpenseResponseDTO> getGroupShareExpenses(UUID userId, ExpenseSearchDTO searchDTO) {
        log.info("=== getGroupShareExpenses 호출 ===");
        log.info("userId: {}, year: {}, month: {}", userId, searchDTO.year(), searchDTO.month());

        User user = findUserById(userId);

        // 1. SHARE 타입 지출들 조회
        List<Expense> shareExpenses = expenseRepository.findShareExpensesByUserAndMonth(user, searchDTO.year(), searchDTO.month());
        log.info("조회된 SHARE 지출 개수: {}", shareExpenses.size());

        return shareExpenses.stream()
                .map(shareExpense -> {
                    log.debug("--- SHARE 지출 처리 중 ---");
                    log.debug("SHARE 지출 ID: {}", shareExpense.getExpenseId());
                    log.debug("SHARE 지출 제목: {}", shareExpense.getTitle());
                    log.debug("SHARE 분담 금액: {}", shareExpense.getAmount());
                    log.debug("원본 지출 ID: {}", shareExpense.getOriginalExpense() != null ? shareExpense.getOriginalExpense().getExpenseId() : null);

                    // 2. 원본 그룹 지출 조회 (더 많은 정보를 위해)
                    Expense originalExpense = shareExpense.getOriginalExpense();

                    // 3. 분할 정보 조회 (원본 지출 기준)
                    List<SplitDataDTO> splitData = null;
                    if (originalExpense != null) {
                        splitData = getSplitData(originalExpense);
                    }

                    // 4. 그룹 이름 조회
                    String groupName = shareExpense.getGroup() != null ?
                            shareExpense.getGroup().getGroupName() : "";
                    log.debug("그룹 이름: {}", groupName);


                    GroupShareExpenseResponseDTO result = expenseConverter.toGroupShareExpenseResponseDTO(
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

        User user = findUserById(userId);
        // 권한 체크 포함해서 지출 조회
        Expense expense = expenseRepository.findByExpenseIdWithPermission(expenseId, user).orElseThrow(
                () -> new CustomException(ErrorCode.EXPENSE_NOT_FOUND)
        );

        // 영수증 파일 없으면 null 반환
        ImageFile receiptImageFile = expense.getReceiptImageFile();
        if (receiptImageFile == null) {
            return null;
        }

        return imageService.generatePresignedUrl(receiptImageFile.getFileId());
    }

    /**
     * 영수증만 삭제 (지출은 유지)
     */
    @Override
    @Transactional
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
     * 개인 지출 월별 추이 조회
     */
    @Override
    public MonthlyExpenseTrendResponseDTO getPersonalExpenseTrend(UUID userId, ExpenseSearchDTO searchDTO) {
        log.info("개인 지출 추이 조회: userId={}, year={}, month={}, months={}, category={}", userId, searchDTO.year(), searchDTO.month(), searchDTO.months(), searchDTO.category());

        User user = findUserById(userId);

        // 날짜 범위 계산
        DateRange dateRange = calculateDateRange(searchDTO.year(), searchDTO.month(), searchDTO.months());

        ExpenseCategory categoryEnum = searchDTO.category() != null ? ExpenseCategory.valueOf(searchDTO.category().toUpperCase()) : null;
        // Repository 조회
        List<Expense> expenses = expenseRepository.findPersonalExpensesByMonthRange(user, dateRange.startDate(), dateRange.endDate(), categoryEnum);

        // 월별 그룹핑 및 응답
        return createTrendResponse(expenses, dateRange);
    }

    /**
     * 그룹 지출 월별 추이 조회
     */
    @Override
    public MonthlyExpenseTrendResponseDTO getGroupExpenseTrend(UUID userId, UUID groupId, ExpenseSearchDTO searchDTO) {
        User user = findUserById(userId);
        Group group = findGroupById(groupId);

        // 그룹 멤버인지 확인
        boolean isMember = groupMemberRepository.existsByGroupAndUser(group, user);
        if (!isMember) {
            throw new CustomException(ErrorCode.GROUP_MEMBER_NOT_FOUND);
        }

        log.info("그룹 지출 추이 조회: groupId={}, year={}, month={}, months={}, category={}",
                groupId, searchDTO.year(), searchDTO.month(), searchDTO.months(), searchDTO.category());

        // 날짜 범위 계산
        DateRange dateRange = calculateDateRange(searchDTO.year(), searchDTO.month(), searchDTO.months());

        ExpenseCategory categoryEnum = searchDTO.category() != null ? ExpenseCategory.valueOf(searchDTO.category().toUpperCase()) : null;
        // Repository에서 데이터 조회
        List<Expense> expenses = expenseRepository.findGroupExpensesByMonthRange(group, dateRange.startDate(), dateRange.endDate(), categoryEnum);

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
//        return MonthlyExpenseTrendResponseDTO.create(monthlyData, dateRange.startDate, dateRange.endDate);
        return expenseConverter.toMonthlyTrendResponseDTO(monthlyData, dateRange.startDate, dateRange.endDate);
    }

    /**
     * 지출 데이터를 월별로 그룹핑
     */
    private Map<String, List<ExpenseResponseDTO>> createMonthlyGrouping(List<Expense> expenses, DateRange dateRange) {
        // N+1 문제 해결: 모든 분담 내역을 한 번에 조회
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

        // 지출 데이터 월별로 그룹핑
        final Map<Long, List<SplitDataDTO>> finalSplitsMap = splitsByExpenseId;
        Map<String, List<ExpenseResponseDTO>> monthlyData = expenses.stream()
                .collect(Collectors.groupingBy(
                        expense -> expense.getExpenseDate().format(DateTimeFormatter.ofPattern("yyyy-MM")),
                        LinkedHashMap::new,  // 순서 보장
                        Collectors.mapping(
                                expense -> expenseConverter.toResponseDTO(expense, finalSplitsMap.get(expense.getExpenseId())),
                                Collectors.toList()
                        )
                ));

        // 데이터 없는 달 빈 배열로 추가
        fillEmptyMonths(monthlyData, dateRange.startDate, dateRange.endDate);
        return sortByMonthKey(monthlyData);
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
     * 그룹 분담 지출 통계 관련 메서드
     */
    @Override
    public PageWithSummaryResponse<GroupShareExpenseResponseDTO> getGroupShareExpensesWithPaging(
            UUID userId, UUID groupId, ExpenseSearchDTO searchDTO, Pageable pageable
    ) {
        User user = findUserById(userId);
        Group group = findGroupById(groupId);

        ExpenseCategory categoryEnum = parseCategory(searchDTO.category());
        // 1. 필터링이 적용된 페이징 조회
        Page<Expense> shareExpensesPage = expenseRepository.findShareExpensesByGroupAndMonthWithPagingFiltered(
                user, group, searchDTO.year(), searchDTO.month(),
                categoryEnum, searchDTO.search(), pageable
        );

        // 2. DTO 변환 (N+1 해결)
        List<GroupShareExpenseResponseDTO> pageContent = convertShareExpensesToDTOs(shareExpensesPage.getContent());
        Page<GroupShareExpenseResponseDTO> shareExpenseResponsePage = new PageImpl<>(pageContent, pageable, shareExpensesPage.getTotalElements());


        // 3. 통계 계산을 위한 전체 데이터 조회
        if (!shareExpenseResponsePage.hasContent()) {
            return PageWithSummaryResponse.of(shareExpenseResponsePage, ExpenseSummary.empty());
        }

        // 전체 데이터 조회 (페이징 없이, 필터링 적용)
        List<Expense> allShareExpenses = expenseRepository.findShareExpensesByGroupAndMonthWithPagingFiltered(
                user, group, searchDTO.year(), searchDTO.month(),
                categoryEnum, searchDTO.search(), Pageable.unpaged()
        ).getContent();

        List<GroupShareExpenseResponseDTO> allShareExpensesDTOs = convertShareExpensesToDTOs(allShareExpenses);

        // 4. 통계 정보 계산
        ExpenseSummary summary = calculateShareExpensesSummary(allShareExpensesDTOs);

        return PageWithSummaryResponse.of(shareExpenseResponsePage, summary);
    }

    /**
     * Expense 리스트를 GroupShareExpenseResponseDTO 리스트로 변환 (N+1 문제 해결)
     */
    private List<GroupShareExpenseResponseDTO> convertShareExpensesToDTOs(List<Expense> shareExpenses) {
        if (shareExpenses.isEmpty()) {
            return Collections.emptyList();
        }

        // 1. 모든 원본 지출 객체를 수집
        List<Expense> originalExpenses = shareExpenses.stream()
                .map(Expense::getOriginalExpense)
                .filter(Objects::nonNull)
                .distinct() // 중복 제거
                .toList();

        // 2. 모든 원본 지출의 분담 내역을 한 번에 조회
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

        // 3. DTO 변환
        return shareExpenses.stream()
                .map(shareExpense -> {
                    Expense originalExpense = shareExpense.getOriginalExpense();
                    String groupName = shareExpense.getGroup() != null ? shareExpense.getGroup().getGroupName() : "";
                    List<SplitDataDTO> splitData = (originalExpense != null) ?
                            splitsByOriginalExpenseId.getOrDefault(originalExpense.getExpenseId(), Collections.emptyList()) :
                            Collections.emptyList();

//                    return GroupShareExpenseResponseDTO.from(
//                            shareExpense,
//                            originalExpense,
//                            groupName,
//                            splitData
//                    );
                    return expenseConverter.toGroupShareExpenseResponseDTO(shareExpense, originalExpense, groupName, splitData);
                })
                .toList();
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

        // 최대 지출 찾기 추가
        GroupShareExpenseResponseDTO maxExpense = expenses.stream()
                .max(Comparator.comparing(GroupShareExpenseResponseDTO::amount))
                .orElse(null);

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
                maxExpense.amount(),
                totalCount,
                myTotalShareAmount,
                myShareCount,
                categoryStats,
                maxExpense.expenseId(),
                maxExpense.title()
        );
    }

    @Override
    public PageWithSummaryResponse<CombinedExpenseResponseDTO> getCombinedExpenses(UUID userId, ExpenseSearchDTO searchDTO, Pageable pageable) {

        User user = findUserById(userId);

        // 1. 개인 지출과 분담 지출 DTO 리스트를 각각 생성
        List<CombinedExpenseResponseDTO> personalExpenseDTOs = fetchPersonalExpenseDTOs(user, searchDTO);
        List<CombinedExpenseResponseDTO> shareExpenseDTOs = fetchShareExpenseDTOs(user, searchDTO);

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
    private List<CombinedExpenseResponseDTO> fetchPersonalExpenseDTOs(User user, ExpenseSearchDTO searchDTO) {

        ExpenseCategory categoryEnum = parseCategory(searchDTO.category());
        List<Expense> personalExpenses = expenseRepository.findPersonalExpensesForCombined(
                user, searchDTO.year(), searchDTO.month(), categoryEnum, searchDTO.search(), Pageable.unpaged()
        ).getContent();

        return personalExpenses.stream()
//                .map(CombinedExpenseResponseDTO::fromPersonal)
                .map(expenseConverter::toCombinedResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * 사용자의 그룹 분담 지출 내역을 조회하여 DTO 리스트 변환 (N+1 해결)
     */
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
//                    return CombinedExpenseResponseDTO.fromGroupShare(shareExpense, originalExpense, groupName);
                    return expenseConverter.toCombinedResponseDTO(shareExpense, originalExpense, groupName);
                })
                .collect(Collectors.toList());
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
    private void validateGroupMembership(User user, Group group) {
        if (!groupMemberRepository.existsByGroupAndUser(group, user)) {
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

        // 최대 지출 찾기 추가
        CombinedExpenseResponseDTO maxExpense = expenses.stream()
                .max(Comparator.comparing(CombinedExpenseResponseDTO::myShareAmount))
                .orElse(null);

        long totalCount = expenses.size();


        // 카테고리별 통계 계산
        Map<String, CategoryStats> categoryStats = calculateCategoryStats(
                expenses.stream().map(e -> new ExpenseStatItem(e.category().name(), e.myShareAmount())).toList(),
                totalAmount
        );

        // 개인/통합 지출의 경우 totalAmount와 myTotalShareAmount가 동일
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
            UUID userId, UUID groupId, ExpenseSearchDTO searchDTO, Pageable pageable
    ) {
        User user = findUserById(userId);
        Group group = findGroupById(groupId);

        // 1. 사전 조건 검증
        validateGroupMembership(user, group);

        // 2. 페이징된 데이터 조회 (기존 fetchGroupExpenses 메서드 활용)
        Page<Expense> groupExpensesPage = fetchGroupExpenses(group, searchDTO, pageable);

        if (!groupExpensesPage.hasContent()) {
            return PageWithSummaryResponse.of(Page.empty(pageable), ExpenseSummary.empty());
        }

        // 3. Page<Expense>를 Page<ExpenseResponseDTO>로 변환
        List<ExpenseResponseDTO> pageContent = convertToExpenseResponseDTOs(groupExpensesPage.getContent());
        Page<ExpenseResponseDTO> expenseResponsePage = new PageImpl<>(pageContent, pageable, groupExpensesPage.getTotalElements());


        // 4. 통계 계산을 위한 전체 데이터 조회 (기존 fetchGroupExpenses 활용)
        List<Expense> allExpensesForStats = fetchGroupExpenses(group, searchDTO, Pageable.unpaged()).getContent();
        List<ExpenseResponseDTO> allExpenseDTOsForStats = convertToExpenseResponseDTOs(allExpensesForStats);


        // 5. 통계 정보 계산
        ExpenseSummary summary = calculateGroupExpensesSummary(allExpenseDTOsForStats);

        return PageWithSummaryResponse.of(expenseResponsePage, summary);
    }

    /**
     * 지출 목록을 DTO로 변환
     */
    private List<ExpenseResponseDTO> convertToExpenseResponseDTOs(List<Expense> expenses) {
        return expenses.stream()
                .map(expense -> {
                    List<SplitDataDTO> splitData = getSplitData(expense);
                    return expenseConverter.toResponseDTO(expense, splitData);
                })
                .collect(Collectors.toList());
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

        // 최대 지출 찾기 추가
        ExpenseResponseDTO maxExpense = expenses.stream()
                .max(Comparator.comparing(ExpenseResponseDTO::amount))
                .orElse(null);

        long totalCount = expenses.size();

        // 기존 calculateCategoryStats 메서드 활용
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


