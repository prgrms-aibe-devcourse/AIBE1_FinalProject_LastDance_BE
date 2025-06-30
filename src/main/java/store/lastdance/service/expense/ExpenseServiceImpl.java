package store.lastdance.service.expense;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import store.lastdance.domain.expense.*;
import store.lastdance.domain.group.Group;
import store.lastdance.domain.group.GroupMember;
import store.lastdance.dto.expense.*;
import store.lastdance.exception.CustomException;
import store.lastdance.exception.ErrorCode;
import store.lastdance.repository.expense.ExpenseRepository;
import store.lastdance.repository.expense.ExpenseSplitRepository;
import store.lastdance.repository.group.GroupMemberRepository;
import store.lastdance.repository.group.GroupRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ExpenseServiceImpl implements ExpenseService {
    private final ExpenseRepository expenseRepository;
    private final ExpenseSplitRepository expenseSplitRepository;
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;

    @Override
    @Transactional
    public ExpenseResponseDTO createExpense(UUID userId, CreateExpenseRequestDTO requestDTO) {
        // ExpenseType (GROUP / PERSONAL)
        ExpenseType expenseType = requestDTO.groupId() != null ? ExpenseType.GROUP : ExpenseType.PERSONAL;

        // 원본 지출
        Expense expense = Expense.builder()
                .title(requestDTO.title())
                .amount(requestDTO.amount())
                .category(requestDTO.category())
                .expenseType(expenseType)
                .userId(userId)
                .expenseDate(requestDTO.date())
                .build();

        // 메모와 그룹ID 설정
        if (requestDTO.memo() != null) {
            expense.updateMemo(requestDTO.memo());
        }
        if (requestDTO.groupId() != null) {
            expense.setGroupId(requestDTO.groupId());

            if (requestDTO.splitType() != null) {
                SplitType splitType = SplitType.valueOf(requestDTO.splitType().toUpperCase());
                expense.setSplitType(splitType);
            }
        }

        Expense savedExpense = expenseRepository.save(expense);

        //그룹 지출의 경우 정산 처리
        if (requestDTO.groupId() != null && requestDTO.splitType() != null) {
            processGroupExpenseSplit(savedExpense, requestDTO);
        }
        return ExpenseResponseDTO.from(savedExpense);
    }

    /**
     * 그룹 지출 정산 처리
     */
    private void processGroupExpenseSplit(Expense original, CreateExpenseRequestDTO dto) {
        // 그룹 정보 조회
        Group group = groupRepository.findById(dto.groupId()).orElseThrow(
                () -> new CustomException(ErrorCode.GROUP_NOT_FOUND)
        );

        // 그룹 멤버 조회
        List<GroupMember> groupMembers = groupMemberRepository.findByGroupId(dto.groupId());
        if (groupMembers.isEmpty()) {
            throw new CustomException(ErrorCode.GROUP_MEMBER_NOT_FOUND);
        }

        SplitType splitType = SplitType.valueOf(dto.splitType().toUpperCase());
        switch (splitType) {
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

//            // 각 개인 가계부 SHARE 타입 생성
//            if (!split.userId().equals(original.getUserId())) {
//                createShareExpense(original, split.userId(), split.amount());
//            }
            // 모든 멤버에게 SHARE 타입 지출 생성 (작성자 포함)
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

//            // 각 멤버 개인 가계부에 SHARE 타입 지출 생성
//            if (!member.getUser().getUserId().equals(original.getUserId())) {
//                createShareExpense(original, member.getUser().getUserId(), splitAmount);
//            }
            // 모든 멤버에게 SHARE 타입 지출 생성 (작성자 포함)
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

    @Override
    public ExpenseResponseDTO getExpenseById(UUID userId, Long expenseId) {
        Expense expense = expenseRepository.findByExpenseIdAndUserId(expenseId, userId).orElseThrow(
                () -> new CustomException(ErrorCode.EXPENSE_NOT_FOUND)
        );
        // 그룹 지출 - 정산 데이터 포함
        List<SplitDataDTO> splitData = null;
        if (expense.getExpenseType() == ExpenseType.GROUP) {
            splitData = getSplitData(expenseId);
        }

        return ExpenseResponseDTO.from(expense, splitData);
    }

    @Override
    @Transactional
    public ExpenseResponseDTO updateExpense(UUID userId, Long expenseId, UpdateExpenseRequestDTO requestDTO) {
        Expense expense = expenseRepository.findByExpenseIdAndUserId(expenseId, userId).orElseThrow(
                () -> new CustomException(ErrorCode.EXPENSE_NOT_FOUND)
        );

        expense.updateTitle(requestDTO.title());
        expense.updateAmount(requestDTO.amount());
        expense.updateCategory(requestDTO.category());
        expense.updateMemo(requestDTO.memo());
        expense.updateExpenseDate(requestDTO.date());

        return ExpenseResponseDTO.from(expense);
    }

    @Override
    @Transactional
    public void deleteExpense(UUID userId, Long expenseId) {
        Expense expense = expenseRepository.findByExpenseIdAndUserId(expenseId, userId).orElseThrow(
                () -> new CustomException(ErrorCode.EXPENSE_NOT_FOUND)
        );

        // 그룹 지출인 경우 연관 데이터 정리
        if (expense.getExpenseType() == ExpenseType.GROUP) {
            expenseSplitRepository.deleteByExpenseId(expenseId);
            expenseRepository.deleteByOriginalExpenseId(expenseId);
        }

        expenseRepository.deleteById(expenseId);
    }

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
}
