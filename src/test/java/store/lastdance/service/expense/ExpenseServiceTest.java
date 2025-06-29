package store.lastdance.service.expense;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import store.lastdance.domain.expense.Expense;
import store.lastdance.domain.expense.ExpenseCategory;
import store.lastdance.domain.expense.ExpenseType;
import store.lastdance.domain.group.Group;
import store.lastdance.domain.group.GroupMember;
import store.lastdance.domain.user.User;
import store.lastdance.domain.user.OAuthProvider;
import store.lastdance.dto.expense.CreateExpenseRequestDTO;
import store.lastdance.dto.expense.ExpenseResponseDTO;
import store.lastdance.dto.expense.UpdateExpenseRequestDTO;
import store.lastdance.exception.CustomException;
import store.lastdance.exception.ErrorCode;
import store.lastdance.repository.expense.ExpenseRepository;
import store.lastdance.repository.expense.ExpenseSplitRepository;
import store.lastdance.repository.group.GroupRepository;
import store.lastdance.repository.group.GroupMemberRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExpenseService 테스트")
class ExpenseServiceTest {

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private GroupMemberRepository groupMemberRepository;

    @Mock
    private ExpenseSplitRepository expenseSplitRepository;

    @InjectMocks
    private ExpenseServiceImpl expenseService;

    private UUID userId;
    private UUID groupId;
    private CreateExpenseRequestDTO createRequestDTO;
    private UpdateExpenseRequestDTO updateRequestDTO;
    private Expense expense;
    private User testUser;
    private Group testGroup;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        groupId = UUID.randomUUID();

        // User 생성
        testUser = createTestUser("test@example.com", "testuser", "테스트유저", UUID.randomUUID());

        // Group 생성
        testGroup = Group.builder()
                .groupName("테스트 그룹")
                .inviteCode("ABC123")
                .owner(testUser)
                .maxMembers(10)
                .groupBudget(1000000)
                .build();

        createRequestDTO = new CreateExpenseRequestDTO(
                "점심식사",
                new BigDecimal("15000"),
                ExpenseCategory.FOOD,
                LocalDate.of(2025, 1, 15),
                "치킨집에서 점심",
                null,  // groupId
                null,  // splitType
                null   // splitData
        );

        updateRequestDTO = new UpdateExpenseRequestDTO(
                "저녁식사",
                new BigDecimal("25000"),
                ExpenseCategory.FOOD,
                LocalDate.of(2025, 1, 15),
                "회식비"
        );

        expense = Expense.builder()
                .title("점심식사")
                .amount(new BigDecimal("15000"))
                .category(ExpenseCategory.FOOD)
                .expenseType(ExpenseType.PERSONAL)
                .userId(userId)
                .expenseDate(LocalDate.of(2025, 1, 15))
                .build();
    }

    // 테스트용 User 생성 헬퍼 메서드
    private User createTestUser(String email, String username, String nickname, UUID userId) {
        User user = User.builder()
                .email(email)
                .username(username)
                .nickname(nickname)
                .provider(OAuthProvider.GOOGLE)
                .providerId(UUID.randomUUID().toString())
                .build();

        // Reflection을 사용해서 userId 설정
        try {
            java.lang.reflect.Field userIdField = User.class.getDeclaredField("userId");
            userIdField.setAccessible(true);
            userIdField.set(user, userId);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set userId for test", e);
        }

        return user;
    }

    // 테스트용 Expense 생성 헬퍼 메서드 (expenseId 포함)
    private Expense createTestExpense(String title, BigDecimal amount, ExpenseType type, UUID userId, Long expenseId) {
        Expense expense = Expense.builder()
                .title(title)
                .amount(amount)
                .category(ExpenseCategory.FOOD)
                .expenseType(type)
                .userId(userId)
                .expenseDate(LocalDate.of(2025, 1, 15))
                .build();

        // Reflection을 사용해서 expenseId 설정
        try {
            java.lang.reflect.Field expenseIdField = Expense.class.getDeclaredField("expenseId");
            expenseIdField.setAccessible(true);
            expenseIdField.set(expense, expenseId);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set expenseId for test", e);
        }

        return expense;
    }

    @Test
    @DisplayName("개인 지출 생성 성공")
    void createPersonalExpense_Success() {
        // given
        given(expenseRepository.save(any(Expense.class))).willReturn(expense);

        // when
        ExpenseResponseDTO result = expenseService.createExpense(userId, createRequestDTO);

        // then
        assertThat(result.title()).isEqualTo("점심식사");
        assertThat(result.amount()).isEqualTo(new BigDecimal("15000"));
        assertThat(result.category()).isEqualTo(ExpenseCategory.FOOD);
        assertThat(result.userId()).isEqualTo(userId);
        assertThat(result.groupId()).isNull();

        then(expenseRepository).should(times(1)).save(any(Expense.class));
    }

    @Test
    @DisplayName("그룹 지출 생성 성공 - 균등분할")
    void createGroupExpense_EqualSplit_Success() {
        // given
        CreateExpenseRequestDTO groupRequestDTO = new CreateExpenseRequestDTO(
                "회식비",
                new BigDecimal("50000"),
                ExpenseCategory.FOOD,
                LocalDate.of(2025, 1, 15),
                "팀 회식",
                groupId,
                "equal",
                null
        );

        // 테스트용 User들 생성 (UUID 포함)
        UUID user1Id = UUID.randomUUID();
        UUID user2Id = UUID.randomUUID();
        UUID expenseCreatorId = userId;

        User user1 = createTestUser("user1@example.com", "user1", "유저1", user1Id);
        User user2 = createTestUser("user2@example.com", "user2", "유저2", user2Id);
        User expenseCreator = createTestUser("creator@example.com", "creator", "지출생성자", expenseCreatorId);

        List<GroupMember> members = List.of(
                GroupMember.builder().group(testGroup).user(expenseCreator).build(),
                GroupMember.builder().group(testGroup).user(user1).build(),
                GroupMember.builder().group(testGroup).user(user2).build()
        );

        // expenseRepository.save() 호출 시 ID를 자동으로 설정하는 Answer
        given(expenseRepository.save(any(Expense.class))).willAnswer(invocation -> {
            Expense expense = invocation.getArgument(0);
            try {
                java.lang.reflect.Field expenseIdField = Expense.class.getDeclaredField("expenseId");
                expenseIdField.setAccessible(true);
                if (expenseIdField.get(expense) == null) {
                    expenseIdField.set(expense, 1L);
                }
            } catch (Exception e) {
                // 테스트용이므로 간단히 처리
            }
            return expense;
        });

        given(groupRepository.findById(groupId)).willReturn(Optional.of(testGroup));
        given(groupMemberRepository.findByGroupId(groupId)).willReturn(members);
        given(expenseSplitRepository.save(any())).willReturn(null);

        // when
        ExpenseResponseDTO result = expenseService.createExpense(userId, groupRequestDTO);

        // then
        assertThat(result.groupId()).isEqualTo(groupId);
        assertThat(result.title()).isEqualTo("회식비");

        // 검증: 원본 지출 1개 + 분담 지출 2개 (본인 제외하고 user1, user2에게)
        then(expenseRepository).should(times(3)).save(any(Expense.class));
        then(expenseSplitRepository).should(times(3)).save(any());
    }

    @Test
    @DisplayName("그룹 멤버가 없는 경우 예외 발생")
    void createGroupExpense_NoMembers_ThrowsException() {
        // given
        CreateExpenseRequestDTO groupRequestDTO = new CreateExpenseRequestDTO(
                "회식비",
                new BigDecimal("50000"),
                ExpenseCategory.FOOD,
                LocalDate.of(2025, 1, 15),
                "팀 회식",
                groupId,
                "equal",
                null
        );

        given(expenseRepository.save(any(Expense.class))).willReturn(expense);
        given(groupRepository.findById(groupId)).willReturn(Optional.of(testGroup));
        given(groupMemberRepository.findByGroupId(groupId)).willReturn(List.of()); // 빈 리스트

        // when & then
        assertThatThrownBy(() -> expenseService.createExpense(userId, groupRequestDTO))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.GROUP_MEMBER_NOT_FOUND);
    }

    @Test
    @DisplayName("개인 지출 + 분담 지출 조회 성공")
    void getPersonalAndShareExpenses_Success() {
        // given
        List<Expense> expenses = List.of(expense);
        given(expenseRepository.findPersonalAndShareExpensesByMonth(userId, 2025, 1))
                .willReturn(expenses);

        // when
        List<ExpenseResponseDTO> result = expenseService.getExpenses(
                userId, "personal", 2025, 1, null, null, null);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).title()).isEqualTo("점심식사");

        then(expenseRepository).should(times(1))
                .findPersonalAndShareExpensesByMonth(userId, 2025, 1);
    }

    @Test
    @DisplayName("그룹 지출 조회 성공")
    void getGroupExpenses_Success() {
        // given
        List<Expense> expenses = List.of(expense);
        given(expenseRepository.findGroupExpensesByMonth(groupId, 2025, 1))
                .willReturn(expenses);

        // when
        List<ExpenseResponseDTO> result = expenseService.getExpenses(
                userId, "group", 2025, 1, null, null, groupId);

        // then
        assertThat(result).hasSize(1);

        then(expenseRepository).should(times(1))
                .findGroupExpensesByMonth(groupId, 2025, 1);
    }

    @Test
    @DisplayName("카테고리 필터링 조회 성공")
    void getExpensesByCategory_Success() {
        // given
        List<Expense> expenses = List.of(expense);
        given(expenseRepository.findPersonalAndShareExpensesByCategoryAndMonth(
                userId, ExpenseCategory.FOOD, 2025, 1))
                .willReturn(expenses);

        // when
        List<ExpenseResponseDTO> result = expenseService.getExpenses(
                userId, "personal", 2025, 1, "FOOD", null, null);

        // then
        assertThat(result).hasSize(1);

        then(expenseRepository).should(times(1))
                .findPersonalAndShareExpensesByCategoryAndMonth(userId, ExpenseCategory.FOOD, 2025, 1);
    }

    @Test
    @DisplayName("검색어로 조회 성공")
    void getExpensesBySearch_Success() {
        // given
        List<Expense> expenses = List.of(expense);
        given(expenseRepository.findPersonalAndShareExpensesBySearch(userId, "점심", 2025, 1))
                .willReturn(expenses);

        // when
        List<ExpenseResponseDTO> result = expenseService.getExpenses(
                userId, "personal", 2025, 1, null, "점심", null);

        // then
        assertThat(result).hasSize(1);

        then(expenseRepository).should(times(1))
                .findPersonalAndShareExpensesBySearch(userId, "점심", 2025, 1);
    }

    @Test
    @DisplayName("지출 상세 조회 성공")
    void getExpenseById_Success() {
        // given
        Long expenseId = 1L;
        given(expenseRepository.findByExpenseIdAndUserId(expenseId, userId))
                .willReturn(Optional.of(expense));

        // when
        ExpenseResponseDTO result = expenseService.getExpenseById(userId, expenseId);

        // then
        assertThat(result.title()).isEqualTo("점심식사");
        assertThat(result.amount()).isEqualTo(new BigDecimal("15000"));
    }

    @Test
    @DisplayName("존재하지 않는 지출 조회 시 예외 발생")
    void getExpenseById_NotFound_ThrowsException() {
        // given
        Long expenseId = 999L;
        given(expenseRepository.findByExpenseIdAndUserId(expenseId, userId))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> expenseService.getExpenseById(userId, expenseId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EXPENSE_NOT_FOUND);
    }

    @Test
    @DisplayName("지출 수정 성공")
    void updateExpense_Success() {
        // given
        Long expenseId = 1L;
        given(expenseRepository.findByExpenseIdAndUserId(expenseId, userId))
                .willReturn(Optional.of(expense));

        // when
        ExpenseResponseDTO result = expenseService.updateExpense(userId, expenseId, updateRequestDTO);

        // then
        assertThat(result.title()).isEqualTo("저녁식사");
        assertThat(result.amount()).isEqualTo(new BigDecimal("25000"));
    }

    @Test
    @DisplayName("존재하지 않는 지출 수정 시 예외 발생")
    void updateExpense_NotFound_ThrowsException() {
        // given
        Long expenseId = 999L;
        given(expenseRepository.findByExpenseIdAndUserId(expenseId, userId))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> expenseService.updateExpense(userId, expenseId, updateRequestDTO))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EXPENSE_NOT_FOUND);
    }

    @Test
    @DisplayName("지출 삭제 성공")
    void deleteExpense_Success() {
        // given
        Long expenseId = 1L;
        given(expenseRepository.existsByExpenseIdAndUserId(expenseId, userId))
                .willReturn(true);

        // when
        expenseService.deleteExpense(userId, expenseId);

        // then
        then(expenseRepository).should(times(1)).deleteById(expenseId);
    }

    @Test
    @DisplayName("존재하지 않는 지출 삭제 시 예외 발생")
    void deleteExpense_NotFound_ThrowsException() {
        // given
        Long expenseId = 999L;
        given(expenseRepository.existsByExpenseIdAndUserId(expenseId, userId))
                .willReturn(false);

        // when & then
        assertThatThrownBy(() -> expenseService.deleteExpense(userId, expenseId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EXPENSE_NOT_FOUND);
    }
}