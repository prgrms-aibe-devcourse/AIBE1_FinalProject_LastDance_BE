package store.lastdance.service.expense;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;
import store.lastdance.domain.common.ImageFile;
import store.lastdance.domain.expense.*;
import store.lastdance.domain.group.Group;
import store.lastdance.domain.group.GroupMember;
import store.lastdance.domain.user.User;
import store.lastdance.domain.user.OAuthProvider;
import store.lastdance.dto.expense.*;
import store.lastdance.exception.CustomException;
import store.lastdance.exception.ErrorCode;
import store.lastdance.repository.expense.ExpenseRepository;
import store.lastdance.repository.expense.ExpenseSplitRepository;
import store.lastdance.repository.group.GroupMemberRepository;
import store.lastdance.repository.group.GroupRepository;
import store.lastdance.service.image.ImageService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
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
    @Mock
    private ImageService imageService;

    @InjectMocks
    private ExpenseServiceImpl expenseService;

    private UUID userId;
    private UUID otherUserId;
    private UUID groupId;
    private CreatePersonalExpenseRequestDTO createPersonalRequestDTO;
    private CreateGroupExpenseRequestDTO createGroupRequestDTO;
    private UpdateExpenseRequestDTO updateRequestDTO;
    private Expense personalExpense;
    private Expense groupExpense;
    private User testUser;
    private User otherUser;
    private Group testGroup;
    private MultipartFile mockFile;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        otherUserId = UUID.randomUUID();
        groupId = UUID.randomUUID();
        mockFile = mock(MultipartFile.class);

        testUser = createTestUser("test@example.com", "testuser", "테스트유저", userId);
        otherUser = createTestUser("other@example.com", "otheruser", "다른유저", otherUserId);

        testGroup = Group.builder()
                .groupName("테스트 그룹")
                .inviteCode("ABC123")
                .owner(testUser)
                .maxMembers(10)
                .groupBudget(1000000)
                .build();

        createPersonalRequestDTO = new CreatePersonalExpenseRequestDTO(
                "점심식사",
                new BigDecimal("15000"),
                ExpenseCategory.FOOD,
                LocalDate.of(2025, 1, 15),
                "치킨집에서 점심"
        );

        createGroupRequestDTO = new CreateGroupExpenseRequestDTO(
                "회식비",
                new BigDecimal("50000"),
                ExpenseCategory.FOOD,
                LocalDate.of(2025, 1, 15),
                "팀 회식",
                groupId,
                SplitType.EQUAL,
                null
        );

        updateRequestDTO = new UpdateExpenseRequestDTO(
                "저녁식사",
                new BigDecimal("25000"),
                ExpenseCategory.FOOD,
                LocalDate.of(2025, 1, 15),
                "회식비",
                null,
                SplitType.EQUAL
        );

        personalExpense = createTestExpense("점심식사", new BigDecimal("15000"),
                ExpenseType.PERSONAL, userId, 1L);

        groupExpense = createTestExpense("회식비", new BigDecimal("50000"),
                ExpenseType.GROUP, userId, 2L);
        groupExpense.setGroupId(groupId);
    }

    private User createTestUser(String email, String username, String nickname, UUID userId) {
        User user = User.builder()
                .email(email)
                .username(username)
                .nickname(nickname)
                .provider(OAuthProvider.GOOGLE)
                .providerId(UUID.randomUUID().toString())
                .build();
        try {
            java.lang.reflect.Field userIdField = User.class.getDeclaredField("userId");
            userIdField.setAccessible(true);
            userIdField.set(user, userId);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set userId for test", e);
        }
        return user;
    }

    private Expense createTestExpense(String title, BigDecimal amount, ExpenseType type, UUID userId, Long expenseId) {
        Expense expense = Expense.builder()
                .title(title)
                .amount(amount)
                .category(ExpenseCategory.FOOD)
                .expenseType(type)
                .userId(userId)
                .expenseDate(LocalDate.of(2025, 1, 15))
                .build();
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
        given(expenseRepository.save(any(Expense.class))).willReturn(personalExpense);

        ExpenseResponseDTO result = expenseService.createPersonalExpense(userId, createPersonalRequestDTO, null);

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
                // ignore
            }
            return expense;
        });

        given(groupRepository.findById(groupId)).willReturn(Optional.of(testGroup));
        given(groupMemberRepository.findByGroupId(groupId)).willReturn(members);
        given(expenseSplitRepository.save(any())).willReturn(null);

        ExpenseResponseDTO result = expenseService.createGroupExpense(userId, createGroupRequestDTO, null);

        assertThat(result.groupId()).isEqualTo(groupId);
        assertThat(result.title()).isEqualTo("회식비");
        then(expenseRepository).should(times(3)).save(any(Expense.class));
        then(expenseSplitRepository).should(times(2)).save(any());
    }

    @Test
    @DisplayName("개인 지출 조회 성공 - 작성자")
    void getPersonalExpenseById_Owner_Success() {
        Long expenseId = 1L;
        given(expenseRepository.findByExpenseIdWithPermission(expenseId, userId))
                .willReturn(Optional.of(personalExpense));

        ExpenseResponseDTO result = expenseService.getExpenseById(userId, expenseId);

        assertThat(result.title()).isEqualTo("점심식사");
        assertThat(result.amount()).isEqualTo(new BigDecimal("15000"));
        assertThat(result.expenseType()).isEqualTo(ExpenseType.PERSONAL);
        then(expenseRepository).should(times(1))
                .findByExpenseIdWithPermission(expenseId, userId);
    }

    @Test
    @DisplayName("개인 지출 조회 실패 - 다른 사용자")
    void getPersonalExpenseById_OtherUser_Fail() {
        Long expenseId = 1L;
        given(expenseRepository.findByExpenseIdWithPermission(expenseId, otherUserId))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> expenseService.getExpenseById(otherUserId, expenseId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EXPENSE_NOT_FOUND);
    }

    @Test
    @DisplayName("그룹 지출 조회 성공 - 작성자")
    void getGroupExpenseById_Owner_Success() {
        Long expenseId = 2L;
        given(expenseRepository.findByExpenseIdWithPermission(expenseId, userId))
                .willReturn(Optional.of(groupExpense));
        given(expenseSplitRepository.findByExpenseId(expenseId)).willReturn(List.of());

        ExpenseResponseDTO result = expenseService.getExpenseById(userId, expenseId);

        assertThat(result.title()).isEqualTo("회식비");
        assertThat(result.expenseType()).isEqualTo(ExpenseType.GROUP);
        assertThat(result.groupId()).isEqualTo(groupId);
    }

    @Test
    @DisplayName("그룹 지출 조회 성공 - 그룹 멤버")
    void getGroupExpenseById_GroupMember_Success() {
        Long expenseId = 2L;
        given(expenseRepository.findByExpenseIdWithPermission(expenseId, otherUserId))
                .willReturn(Optional.of(groupExpense));

        ExpenseResponseDTO result = expenseService.getExpenseById(otherUserId, expenseId);

        assertThat(result.title()).isEqualTo("회식비");
        assertThat(result.expenseType()).isEqualTo(ExpenseType.GROUP);
        then(expenseRepository).should(times(1))
                .findByExpenseIdWithPermission(expenseId, otherUserId);
    }

    @Test
    @DisplayName("그룹 지출 조회 실패 - 비그룹 멤버")
    void getGroupExpenseById_NonGroupMember_Fail() {
        Long expenseId = 2L;
        UUID nonMemberUserId = UUID.randomUUID();
        given(expenseRepository.findByExpenseIdWithPermission(expenseId, nonMemberUserId))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> expenseService.getExpenseById(nonMemberUserId, expenseId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EXPENSE_NOT_FOUND);
    }

    @Test
    @DisplayName("개인 지출 수정 성공 - 작성자")
    void updatePersonalExpense_Owner_Success() {
        Long expenseId = 1L;
        given(expenseRepository.findByExpenseIdWithPermission(expenseId, userId))
                .willReturn(Optional.of(personalExpense));

        ExpenseResponseDTO result = expenseService.updateExpense(userId, expenseId, updateRequestDTO, null);

        assertThat(result.title()).isEqualTo("저녁식사");
        assertThat(result.amount()).isEqualTo(new BigDecimal("25000"));
    }

    @Test
    @DisplayName("개인 지출 수정 실패 - 다른 사용자")
    void updatePersonalExpense_OtherUser_Fail() {
        Long expenseId = 1L;
        given(expenseRepository.findByExpenseIdWithPermission(expenseId, otherUserId))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> expenseService.updateExpense(otherUserId, expenseId, updateRequestDTO, null))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EXPENSE_NOT_FOUND);
    }

    @Test
    @DisplayName("그룹 지출 수정 성공 - 그룹 멤버")
    void updateGroupExpense_GroupMember_Success() {
        Long expenseId = 2L;
        groupExpense.setSplitType(SplitType.EQUAL);
        ExpenseSplit split = ExpenseSplit.builder().expenseId(expenseId).userId(userId).amount(new BigDecimal("25000")).build();

        given(expenseRepository.findByExpenseIdWithPermission(expenseId, otherUserId))
                .willReturn(Optional.of(groupExpense));
        given(expenseSplitRepository.findByExpenseId(anyLong())).willReturn(List.of(split));

        ExpenseResponseDTO result = expenseService.updateExpense(otherUserId, expenseId, updateRequestDTO, null);

        assertThat(result.title()).isEqualTo("저녁식사");
        assertThat(result.amount()).isEqualTo(new BigDecimal("25000"));
    }

    @Test
    @DisplayName("지출 삭제 성공 - 권한 있는 사용자")
    void deleteExpense_WithPermission_Success() {
        Long expenseId = 1L;
        given(expenseRepository.findByExpenseIdWithPermission(expenseId, userId))
                .willReturn(Optional.of(personalExpense));

        expenseService.deleteExpense(userId, expenseId);

        then(expenseRepository).should(times(1)).deleteById(expenseId);
    }

    @Test
    @DisplayName("지출 삭제 실패 - 권한 없는 사용자")
    void deleteExpense_WithoutPermission_Fail() {
        Long expenseId = 1L;
        given(expenseRepository.findByExpenseIdWithPermission(expenseId, otherUserId))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> expenseService.deleteExpense(otherUserId, expenseId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EXPENSE_NOT_FOUND);
    }

    @Test
    @DisplayName("SHARE 타입 지출 조회/수정/삭제 불가")
    void shareExpense_NotAccessible() {
        Long expenseId = 3L;
        given(expenseRepository.findByExpenseIdWithPermission(expenseId, userId))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> expenseService.getExpenseById(userId, expenseId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EXPENSE_NOT_FOUND);
    }

    @Test
    @DisplayName("개인 지출 목록 조회 성공")
    void getPersonalExpenses_Success() {
        List<Expense> expenses = List.of(personalExpense);
        given(expenseRepository.findPersonalExpensesByMonth(userId, 2025, 1))
                .willReturn(expenses);

        List<ExpenseResponseDTO> result = expenseService.getPersonalExpenses(
                userId, 2025, 1, null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).title()).isEqualTo("점심식사");
        then(expenseRepository).should(times(1))
                .findPersonalExpensesByMonth(userId, 2025, 1);
    }

    @Test
    @DisplayName("그룹 지출 목록 조회 성공")
    void getGroupExpenses_Success() {
        List<Expense> expenses = List.of(groupExpense);
        given(groupMemberRepository.existsByGroupIdAndUserId(groupId, userId))
                .willReturn(true);
        given(expenseRepository.findGroupExpensesByMonth(groupId, 2025, 1))
                .willReturn(expenses);
        given(expenseSplitRepository.findByExpenseId(any())).willReturn(List.of());

        List<ExpenseResponseDTO> result = expenseService.getGroupExpenses(
                userId, groupId, 2025, 1);

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
        given(groupMemberRepository.existsByGroupIdAndUserId(groupId, otherUserId))
                .willReturn(false);

        assertThatThrownBy(() -> expenseService.getGroupExpenses(otherUserId, groupId, 2025, 1))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.GROUP_MEMBER_NOT_FOUND);
    }

    @Test
    @DisplayName("영수증 업로드와 함께 지출 생성 성공")
    void createExpense_WithReceipt_Success() throws Exception {
        given(mockFile.isEmpty()).willReturn(false);
        UUID receiptFileId = UUID.randomUUID();
        ImageFile imageFile = ImageFile.builder()
                .fileId(receiptFileId)
                .originalName("test.jpg")
                .storedName("stored.jpg")
                .filePath("http://s3.url/test.jpg")
                .fileSize(100L)
                .mimeType("image/jpeg")
                .build();

        given(imageService.uploadImageToS3(any(), any(), anyInt())).willReturn(imageFile);
        given(expenseRepository.save(any(Expense.class))).willAnswer(invocation -> {
            Expense expense = invocation.getArgument(0);
            assertThat(expense.getReceiptImageFileId()).isEqualTo(receiptFileId);
            return expense;
        });

        expenseService.createPersonalExpense(userId, createPersonalRequestDTO, mockFile);

        then(imageService).should(times(1)).uploadImageToS3(any(), any(), anyInt());
        then(expenseRepository).should(times(1)).save(any(Expense.class));
    }

    @Test
    @DisplayName("개인 지출 월별 추이 조회 성공")
    void getPersonalExpenseTrend_Success() {
        // Given
        int year = 2025;
        int month = 3;
        int months = 3; // 1월, 2월, 3월
        String category = null;

        // 1월 데이터
        Expense expenseJan1 = createTestExpense("1월 식비", new BigDecimal("10000"), ExpenseType.PERSONAL, userId, 10L);
        expenseJan1.updateExpenseDate(LocalDate.of(2025, 1, 10));
        Expense expenseJan2 = createTestExpense("1월 교통비", new BigDecimal("5000"), ExpenseType.PERSONAL, userId, 11L);
        expenseJan2.updateExpenseDate(LocalDate.of(2025, 1, 20));

        // 2월 데이터
        Expense expenseFeb1 = createTestExpense("2월 식비", new BigDecimal("12000"), ExpenseType.PERSONAL, userId, 12L);
        expenseFeb1.updateExpenseDate(LocalDate.of(2025, 2, 5));

        // 3월 데이터
        Expense expenseMar1 = createTestExpense("3월 식비", new BigDecimal("15000"), ExpenseType.PERSONAL, userId, 13L);
        expenseMar1.updateExpenseDate(LocalDate.of(2025, 3, 15));

        given(expenseRepository.findPersonalExpensesByMonthRange(
                any(UUID.class), any(LocalDate.class), any(LocalDate.class), any()))
                .willReturn(List.of(expenseJan1, expenseJan2, expenseFeb1, expenseMar1));

        // When
        MonthlyExpenseTrendResponseDTO result = expenseService.getPersonalExpenseTrend(userId, year, month, months, category);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.monthlyData()).hasSize(3); // 1월, 2월, 3월
        assertThat(result.monthlyData().get("2025-01")).hasSize(2);
        assertThat(result.monthlyData().get("2025-02")).hasSize(1);
        assertThat(result.monthlyData().get("2025-03")).hasSize(1);

        assertThat(result.monthlyData().get("2025-01").stream()
                .map(ExpenseResponseDTO::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add))
                .isEqualTo(new BigDecimal("15000")); // 10000 + 5000

        assertThat(result.monthlyData().get("2025-02").stream()
                .map(ExpenseResponseDTO::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add))
                .isEqualTo(new BigDecimal("12000"));

        assertThat(result.monthlyData().get("2025-03").stream()
                .map(ExpenseResponseDTO::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add))
                .isEqualTo(new BigDecimal("15000"));

        then(expenseRepository).should(times(1))
                .findPersonalExpensesByMonthRange(any(UUID.class), any(LocalDate.class), any(LocalDate.class), any());
    }

    @Test
    @DisplayName("개인 지출 월별 추이 조회 성공 - 카테고리 필터링")
    void getPersonalExpenseTrend_WithCategory_Success() {
        // Given
        int year = 2025;
        int month = 3;
        int months = 3;
        String category = "FOOD";

        // 1월 식비
        Expense expenseJan1 = createTestExpense("1월 식비", new BigDecimal("10000"), ExpenseType.PERSONAL, userId, 10L);
        expenseJan1.updateExpenseDate(LocalDate.of(2025, 1, 10));
        expenseJan1.updateCategory(ExpenseCategory.FOOD);
        // 1월 교통비 (필터링되어야 함)
        Expense expenseJan2 = createTestExpense("1월 교통비", new BigDecimal("5000"), ExpenseType.PERSONAL, userId, 11L);
        expenseJan2.updateExpenseDate(LocalDate.of(2025, 1, 20));
        expenseJan2.updateCategory(ExpenseCategory.TRANSPORT);

        given(expenseRepository.findPersonalExpensesByMonthRange(
                any(UUID.class), any(LocalDate.class), any(LocalDate.class), eq(ExpenseCategory.FOOD)))
                .willReturn(List.of(expenseJan1));

        // When
        MonthlyExpenseTrendResponseDTO result = expenseService.getPersonalExpenseTrend(userId, year, month, months, category);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.monthlyData()).hasSize(3); // 1월, 2월, 3월 (데이터 없는 달 포함)
        assertThat(result.monthlyData().get("2025-01")).hasSize(1); // FOOD 카테고리만
        assertThat(result.monthlyData().get("2025-01").get(0).title()).isEqualTo("1월 식비");
        assertThat(result.monthlyData().get("2025-02")).isEmpty();
        assertThat(result.monthlyData().get("2025-03")).isEmpty();

        then(expenseRepository).should(times(1))
                .findPersonalExpensesByMonthRange(any(UUID.class), any(LocalDate.class), any(LocalDate.class), any());
    }

    @Test
    @DisplayName("그룹 지출 월별 추이 조회 성공")
    void getGroupExpenseTrend_Success() {
        // Given
        int year = 2025;
        int month = 3;
        int months = 3;
        String category = null;

        // 1월 그룹 지출
        Expense groupExpenseJan1 = createTestExpense("1월 그룹 회식", new BigDecimal("30000"), ExpenseType.GROUP, userId, 20L);
        groupExpenseJan1.updateExpenseDate(LocalDate.of(2025, 1, 10));
        groupExpenseJan1.setGroupId(groupId);

        // 2월 그룹 지출
        Expense groupExpenseFeb1 = createTestExpense("2월 그룹 워크샵", new BigDecimal("50000"), ExpenseType.GROUP, userId, 21L);
        groupExpenseFeb1.updateExpenseDate(LocalDate.of(2025, 2, 15));
        groupExpenseFeb1.setGroupId(groupId);

        given(groupMemberRepository.existsByGroupIdAndUserId(groupId, userId)).willReturn(true);
        given(expenseRepository.findGroupExpensesByMonthRange(
                any(UUID.class), any(LocalDate.class), any(LocalDate.class), any()))
                .willReturn(List.of(groupExpenseJan1, groupExpenseFeb1));
        given(expenseSplitRepository.findByExpenseId(anyLong())).willReturn(List.of()); // 그룹 지출 분할 데이터는 테스트에서 중요하지 않으므로 빈 리스트 반환

        // When
        MonthlyExpenseTrendResponseDTO result = expenseService.getGroupExpenseTrend(userId, groupId, year, month, months, category);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.monthlyData()).hasSize(3); // 1월, 2월, 3월
        assertThat(result.monthlyData().get("2025-01")).hasSize(1);
        assertThat(result.monthlyData().get("2025-02")).hasSize(1);
        assertThat(result.monthlyData().get("2025-03")).isEmpty(); // 3월 데이터는 없음

        assertThat(result.monthlyData().get("2025-01").stream()
                .map(ExpenseResponseDTO::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add))
                .isEqualTo(new BigDecimal("30000"));

        assertThat(result.monthlyData().get("2025-02").stream()
                .map(ExpenseResponseDTO::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add))
                .isEqualTo(new BigDecimal("50000"));

        then(groupMemberRepository).should(times(1)).existsByGroupIdAndUserId(groupId, userId);
        then(expenseRepository).should(times(1))
                .findGroupExpensesByMonthRange(any(UUID.class), any(LocalDate.class), any(LocalDate.class), any());
    }

    @Test
    @DisplayName("그룹 지출 월별 추이 조회 실패 - 그룹 멤버 아님")
    void getGroupExpenseTrend_NotGroupMember_Fail() {
        // Given
        int year = 2025;
        int month = 3;
        int months = 3;
        String category = null;

        given(groupMemberRepository.existsByGroupIdAndUserId(groupId, otherUserId)).willReturn(false);

        // When & Then
        assertThatThrownBy(() -> expenseService.getGroupExpenseTrend(otherUserId, groupId, year, month, months, category))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.GROUP_MEMBER_NOT_FOUND);

        then(groupMemberRepository).should(times(1)).existsByGroupIdAndUserId(groupId, otherUserId);
        then(expenseRepository).should(times(0)) // Repository는 호출되지 않아야 함
                .findGroupExpensesByMonthRange(any(UUID.class), any(LocalDate.class), any(LocalDate.class), any());
    }
}
