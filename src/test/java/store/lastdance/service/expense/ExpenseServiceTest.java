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
    private UUID otherUserId;
    private UUID groupId;
    private CreateExpenseRequestDTO createRequestDTO;
    private UpdateExpenseRequestDTO updateRequestDTO;
    private Expense personalExpense;
    private Expense groupExpense;
    private User testUser;
    private User otherUser;
    private Group testGroup;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        otherUserId = UUID.randomUUID();
        groupId = UUID.randomUUID();

        // User 생성
        testUser = createTestUser("test@example.com", "testuser", "테스트유저", userId);
        otherUser = createTestUser("other@example.com", "otheruser", "다른유저", otherUserId);

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
                "회식비",
                null,
                null
        );

        // 개인 지출
        personalExpense = createTestExpense("점심식사", new BigDecimal("15000"),
                ExpenseType.PERSONAL, userId, 1L);

        // 그룹 지출
        groupExpense = createTestExpense("회식비", new BigDecimal("50000"),
                ExpenseType.GROUP, userId, 2L);
        groupExpense.setGroupId(groupId);
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
        given(expenseRepository.save(any(Expense.class))).willReturn(personalExpense);

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

        List<GroupMember> members = List.of(
                GroupMember.builder().group(testGroup).user(testUser).build(),
                GroupMember.builder().group(testGroup).user(otherUser).build()
        );

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

        // 검증: 원본 지출 1개 + 분담 지출 2개
        then(expenseRepository).should(times(3)).save(any(Expense.class));
        then(expenseSplitRepository).should(times(2)).save(any());
    }

    @Test
    @DisplayName("개인 지출 조회 성공 - 작성자")
    void getPersonalExpenseById_Owner_Success() {
        // given
        Long expenseId = 1L;
        given(expenseRepository.findByExpenseIdWithPermission(expenseId, userId))
                .willReturn(Optional.of(personalExpense));

        // when
        ExpenseResponseDTO result = expenseService.getExpenseById(userId, expenseId);

        // then
        assertThat(result.title()).isEqualTo("점심식사");
        assertThat(result.amount()).isEqualTo(new BigDecimal("15000"));
        assertThat(result.expenseType()).isEqualTo(ExpenseType.PERSONAL);

        then(expenseRepository).should(times(1))
                .findByExpenseIdWithPermission(expenseId, userId);
    }

    @Test
    @DisplayName("개인 지출 조회 실패 - 다른 사용자")
    void getPersonalExpenseById_OtherUser_Fail() {
        // given
        Long expenseId = 1L;
        given(expenseRepository.findByExpenseIdWithPermission(expenseId, otherUserId))
                .willReturn(Optional.empty()); // JOIN 쿼리에서 권한 없으면 빈 결과

        // when & then
        assertThatThrownBy(() -> expenseService.getExpenseById(otherUserId, expenseId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EXPENSE_NOT_FOUND);
    }

    @Test
    @DisplayName("그룹 지출 조회 성공 - 작성자")
    void getGroupExpenseById_Owner_Success() {
        // given
        Long expenseId = 2L;
        given(expenseRepository.findByExpenseIdWithPermission(expenseId, userId))
                .willReturn(Optional.of(groupExpense));
        given(expenseSplitRepository.findByExpenseId(expenseId)).willReturn(List.of());

        // when
        ExpenseResponseDTO result = expenseService.getExpenseById(userId, expenseId);

        // then
        assertThat(result.title()).isEqualTo("회식비");
        assertThat(result.expenseType()).isEqualTo(ExpenseType.GROUP);
        assertThat(result.groupId()).isEqualTo(groupId);
    }

    @Test
    @DisplayName("그룹 지출 조회 성공 - 그룹 멤버")
    void getGroupExpenseById_GroupMember_Success() {
        // given
        Long expenseId = 2L;
        given(expenseRepository.findByExpenseIdWithPermission(expenseId, otherUserId))
                .willReturn(Optional.of(groupExpense)); // 그룹 멤버라면 조회 가능

        // when
        ExpenseResponseDTO result = expenseService.getExpenseById(otherUserId, expenseId);

        // then
        assertThat(result.title()).isEqualTo("회식비");
        assertThat(result.expenseType()).isEqualTo(ExpenseType.GROUP);

        then(expenseRepository).should(times(1))
                .findByExpenseIdWithPermission(expenseId, otherUserId);
    }

    @Test
    @DisplayName("그룹 지출 조회 실패 - 비그룹 멤버")
    void getGroupExpenseById_NonGroupMember_Fail() {
        // given
        Long expenseId = 2L;
        UUID nonMemberUserId = UUID.randomUUID();
        given(expenseRepository.findByExpenseIdWithPermission(expenseId, nonMemberUserId))
                .willReturn(Optional.empty()); // 비그룹 멤버는 조회 불가

        // when & then
        assertThatThrownBy(() -> expenseService.getExpenseById(nonMemberUserId, expenseId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EXPENSE_NOT_FOUND);
    }

    @Test
    @DisplayName("개인 지출 수정 성공 - 작성자")
    void updatePersonalExpense_Owner_Success() {
        // given
        Long expenseId = 1L;
        given(expenseRepository.findByExpenseIdWithPermission(expenseId, userId))
                .willReturn(Optional.of(personalExpense));

        // when
        ExpenseResponseDTO result = expenseService.updateExpense(userId, expenseId, updateRequestDTO);

        // then
        assertThat(result.title()).isEqualTo("저녁식사");
        assertThat(result.amount()).isEqualTo(new BigDecimal("25000"));
    }

    @Test
    @DisplayName("개인 지출 수정 실패 - 다른 사용자")
    void updatePersonalExpense_OtherUser_Fail() {
        // given
        Long expenseId = 1L;
        given(expenseRepository.findByExpenseIdWithPermission(expenseId, otherUserId))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> expenseService.updateExpense(otherUserId, expenseId, updateRequestDTO))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EXPENSE_NOT_FOUND);
    }

    @Test
    @DisplayName("그룹 지출 수정 성공 - 그룹 멤버")
    void updateGroupExpense_GroupMember_Success() {
        // given
        Long expenseId = 2L;
        given(expenseRepository.findByExpenseIdWithPermission(expenseId, otherUserId))
                .willReturn(Optional.of(groupExpense));

        // when
        ExpenseResponseDTO result = expenseService.updateExpense(otherUserId, expenseId, updateRequestDTO);

        // then
        assertThat(result.title()).isEqualTo("저녁식사");
        assertThat(result.amount()).isEqualTo(new BigDecimal("25000"));
    }

    @Test
    @DisplayName("지출 삭제 성공 - 권한 있는 사용자")
    void deleteExpense_WithPermission_Success() {
        // given
        Long expenseId = 1L;
        given(expenseRepository.findByExpenseIdWithPermission(expenseId, userId))
                .willReturn(Optional.of(personalExpense));

        // when
        expenseService.deleteExpense(userId, expenseId);

        // then
        then(expenseRepository).should(times(1)).deleteById(expenseId);
    }

    @Test
    @DisplayName("지출 삭제 실패 - 권한 없는 사용자")
    void deleteExpense_WithoutPermission_Fail() {
        // given
        Long expenseId = 1L;
        given(expenseRepository.findByExpenseIdWithPermission(expenseId, otherUserId))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> expenseService.deleteExpense(otherUserId, expenseId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EXPENSE_NOT_FOUND);
    }

    @Test
    @DisplayName("SHARE 타입 지출 조회/수정/삭제 불가")
    void shareExpense_NotAccessible() {
        // given
        Long expenseId = 3L;
        Expense shareExpense = createTestExpense("분담비", new BigDecimal("10000"),
                ExpenseType.SHARE, userId, expenseId);

        // SHARE 타입은 findByExpenseIdWithPermission에서 제외됨
        given(expenseRepository.findByExpenseIdWithPermission(expenseId, userId))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> expenseService.getExpenseById(userId, expenseId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EXPENSE_NOT_FOUND);
    }

    @Test
    @DisplayName("개인 지출 목록 조회 성공")
    void getPersonalExpenses_Success() {
        // given
        List<Expense> expenses = List.of(personalExpense);
        given(expenseRepository.findPersonalExpensesByMonth(userId, 2025, 1))
                .willReturn(expenses);

        // when
        List<ExpenseResponseDTO> result = expenseService.getPersonalExpenses(
                userId, 2025, 1, null, null);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).title()).isEqualTo("점심식사");

        then(expenseRepository).should(times(1))
                .findPersonalExpensesByMonth(userId, 2025, 1);
    }

    @Test
    @DisplayName("그룹 지출 목록 조회 성공")
    void getGroupExpenses_Success() {
        // given
        List<Expense> expenses = List.of(groupExpense);
        given(groupMemberRepository.existsByGroupIdAndUserId(groupId, userId))
                .willReturn(true);
        given(expenseRepository.findGroupExpensesByMonth(groupId, 2025, 1))
                .willReturn(expenses);
        given(expenseSplitRepository.findByExpenseId(any())).willReturn(List.of());

        // when
        List<ExpenseResponseDTO> result = expenseService.getGroupExpenses(
                userId, groupId, 2025, 1);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).title()).isEqualTo("회식비");

        then(groupMemberRepository).should(times(1))
                .existsByGroupIdAndUserId(groupId, userId);
        then(expenseRepository).should(times(1))
                .findGroupExpensesByMonth(groupId, 2025, 1);
    }

    @Test
    @DisplayName("그룹 멤버가 아닌 경우 그룹 지출 조회 실패")
    void getGroupExpenses_NotGroupMember_Fail() {
        // given
        given(groupMemberRepository.existsByGroupIdAndUserId(groupId, otherUserId))
                .willReturn(false);

        // when & then
        assertThatThrownBy(() -> expenseService.getGroupExpenses(otherUserId, groupId, 2025, 1))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.GROUP_MEMBER_NOT_FOUND);
    }
}