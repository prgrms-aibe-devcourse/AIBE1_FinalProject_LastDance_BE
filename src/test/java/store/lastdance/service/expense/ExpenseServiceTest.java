package store.lastdance.service.expense;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;
import store.lastdance.domain.common.ImageFile;
import store.lastdance.domain.expense.*;
import store.lastdance.domain.group.Group;
import store.lastdance.domain.group.GroupMember;
import store.lastdance.domain.user.OAuthProvider;
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
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
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
    private UserRepository userRepository;
    @Mock
    private ImageService imageService;

    @InjectMocks
    private ExpenseServiceImpl expenseService;

    private User testUser;
    private User otherUser;
    private Group testGroup;
    private Expense personalExpense;
    private Expense groupExpense;
    private CreatePersonalExpenseRequestDTO createPersonalRequestDTO;
    private CreateGroupExpenseRequestDTO createGroupRequestDTO;
    private UpdateExpenseRequestDTO updateRequestDTO;
    private MultipartFile mockFile;
    private Random random;
    private int randomYear;
    private int randomMonth;

    @BeforeEach
    void setUp() {
        random = new Random();
        randomYear = generateRandomYear();
        randomMonth = generateRandomMonth();

        UUID userId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
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
        try {
            java.lang.reflect.Field groupIdField = Group.class.getDeclaredField("groupId");
            groupIdField.setAccessible(true);
            groupIdField.set(testGroup, groupId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        LocalDate randomDate = generateRandomDate(randomYear, randomMonth);

        createPersonalRequestDTO = new CreatePersonalExpenseRequestDTO(
                "점심식사",
                new BigDecimal("15000"),
                ExpenseCategory.FOOD,
                randomDate,
                "치킨집에서 점심"
        );

        createGroupRequestDTO = new CreateGroupExpenseRequestDTO(
                "회식비",
                new BigDecimal("50000"),
                ExpenseCategory.FOOD,
                randomDate,
                "팀 회식",
                groupId,
                SplitType.EQUAL,
                null
        );

        updateRequestDTO = new UpdateExpenseRequestDTO(
                "저녁식사",
                new BigDecimal("25000"),
                ExpenseCategory.FOOD,
                randomDate,
                "회식비",
                null,
                SplitType.EQUAL
        );

        personalExpense = createTestExpense(1L, "점심식사", new BigDecimal("15000"), ExpenseType.PERSONAL, testUser, randomDate);
        groupExpense = createTestExpense(2L, "회식비", new BigDecimal("50000"), ExpenseType.GROUP, testUser, randomDate);
        groupExpense.updateGroup(testGroup);
    }

    private int generateRandomYear() {
        return LocalDate.now().getYear() - 2 + random.nextInt(5); // Current year +/- 2 years
    }

    private int generateRandomMonth() {
        return random.nextInt(12) + 1; // 1 to 12
    }

    private LocalDate generateRandomDate(int year, int month) {
        int day = random.nextInt(28) + 1; // 1 to 28 to avoid issues with month lengths
        return LocalDate.of(year, month, day);
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

    private Expense createTestExpense(Long expenseId, String title, BigDecimal amount, ExpenseType type, User user, LocalDate expenseDate) {
        Expense expense = Expense.builder()
                .title(title)
                .amount(amount)
                .category(ExpenseCategory.FOOD)
                .expenseType(type)
                .user(user)
                .expenseDate(expenseDate)
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
        given(userRepository.findById(testUser.getUserId())).willReturn(Optional.of(testUser));
        given(expenseRepository.save(any(Expense.class))).willAnswer(invocation -> invocation.getArgument(0));

        ExpenseResponseDTO result = expenseService.createPersonalExpense(testUser.getUserId(), createPersonalRequestDTO, null);

        assertThat(result.title()).isEqualTo("점심식사");
        assertThat(result.amount()).isEqualByComparingTo("15000");
        assertThat(result.category()).isEqualTo(ExpenseCategory.FOOD);
        assertThat(result.userId()).isEqualTo(testUser.getUserId());
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

        given(userRepository.findById(testUser.getUserId())).willReturn(Optional.of(testUser));
        given(groupRepository.findById(testGroup.getGroupId())).willReturn(Optional.of(testGroup));
        given(expenseRepository.save(any(Expense.class))).willAnswer(invocation -> {
            Expense arg = invocation.getArgument(0);
            if (arg.getExpenseType() == ExpenseType.GROUP) {
                try {
                    java.lang.reflect.Field idField = Expense.class.getDeclaredField("expenseId");
                    idField.setAccessible(true);
                    idField.set(arg, 1L);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            return arg;
        });
        given(groupMemberRepository.findByGroup(testGroup)).willReturn(members);

        ExpenseResponseDTO result = expenseService.createGroupExpense(testUser.getUserId(), createGroupRequestDTO, null);

        assertThat(result.groupId()).isEqualTo(testGroup.getGroupId());
        assertThat(result.title()).isEqualTo("회식비");
        then(expenseRepository).should(times(3)).save(any(Expense.class));
        then(expenseSplitRepository).should(times(2)).save(any(ExpenseSplit.class));
    }

    @Test
    @DisplayName("지출 조회 성공")
    void getExpenseById_Success() {
        Long expenseId = 1L;
        given(userRepository.findById(testUser.getUserId())).willReturn(Optional.of(testUser));
        given(expenseRepository.findByExpenseIdWithPermission(expenseId, testUser))
                .willReturn(Optional.of(personalExpense));

        ExpenseResponseDTO result = expenseService.getExpenseById(testUser.getUserId(), expenseId);

        assertThat(result.title()).isEqualTo("점심식사");
        assertThat(result.expenseType()).isEqualTo(ExpenseType.PERSONAL);
    }

    @Test
    @DisplayName("지출 조회 실패 - 권한 없음")
    void getExpenseById_Fail_PermissionDenied() {
        Long expenseId = 1L;
        given(userRepository.findById(otherUser.getUserId())).willReturn(Optional.of(otherUser));
        given(expenseRepository.findByExpenseIdWithPermission(expenseId, otherUser))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> expenseService.getExpenseById(otherUser.getUserId(), expenseId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EXPENSE_NOT_FOUND);
    }

    @Test
    @DisplayName("지출 수정 성공")
    void updateExpense_Success() {
        Long expenseId = 1L;
        given(userRepository.findById(testUser.getUserId())).willReturn(Optional.of(testUser));
        given(expenseRepository.findByExpenseIdWithPermission(expenseId, testUser))
                .willReturn(Optional.of(personalExpense));

        ExpenseResponseDTO result = expenseService.updateExpense(testUser.getUserId(), expenseId, updateRequestDTO, null);

        assertThat(result.title()).isEqualTo("저녁식사");
        assertThat(result.amount()).isEqualByComparingTo("25000");
    }

    @Test
    @DisplayName("그룹 지출 수정 시 분담 내역 재생성")
    void updateGroupExpense_RecreatesSplits() {
        Long expenseId = 2L;
        groupExpense.updateSplitType(SplitType.EQUAL);
        List<GroupMember> members = List.of(
                GroupMember.builder().group(testGroup).user(testUser).build(),
                GroupMember.builder().group(testGroup).user(otherUser).build()
        );

        given(userRepository.findById(testUser.getUserId())).willReturn(Optional.of(testUser));
        given(expenseRepository.findByExpenseIdWithPermission(expenseId, testUser))
                .willReturn(Optional.of(groupExpense));
        given(groupMemberRepository.findByGroup(testGroup)).willReturn(members);

        expenseService.updateExpense(testUser.getUserId(), expenseId, updateRequestDTO, null);

        then(expenseSplitRepository).should(times(1)).deleteByExpense(groupExpense);
        then(expenseRepository).should(times(1)).deleteByOriginalExpense(groupExpense);
        then(expenseSplitRepository).should(times(2)).save(any(ExpenseSplit.class));
        then(expenseRepository).should(times(2)).save(any(Expense.class));
    }

    @Test
    @DisplayName("지출 삭제 성공")
    void deleteExpense_Success() {
        Long expenseId = 1L;
        given(userRepository.findById(testUser.getUserId())).willReturn(Optional.of(testUser));
        given(expenseRepository.findByExpenseIdWithPermission(expenseId, testUser))
                .willReturn(Optional.of(personalExpense));

        expenseService.deleteExpense(testUser.getUserId(), expenseId);

        then(expenseRepository).should(times(1)).deleteById(expenseId);
    }

    @Test
    @DisplayName("그룹 지출 삭제 시 연관 데이터 삭제")
    void deleteGroupExpense_DeletesRelatedData() {
        Long expenseId = 2L;
        given(userRepository.findById(testUser.getUserId())).willReturn(Optional.of(testUser));
        given(expenseRepository.findByExpenseIdWithPermission(expenseId, testUser))
                .willReturn(Optional.of(groupExpense));

        expenseService.deleteExpense(testUser.getUserId(), expenseId);

        then(expenseSplitRepository).should(times(1)).deleteByExpense(groupExpense);
        then(expenseRepository).should(times(1)).deleteByOriginalExpense(groupExpense);
        then(expenseRepository).should(times(1)).deleteById(expenseId);
    }

    @Test
    @DisplayName("통합 지출 목록 조회 성공")
    void getCombinedExpenses_Success() {
        ExpenseSearchDTO searchDTO = new ExpenseSearchDTO(randomYear, randomMonth, null, null, 1);
        Pageable pageable = PageRequest.of(0, 10);

        Expense shareExpense = createTestExpense(3L, "회식비 (그룹 분담)", new BigDecimal("25000"), ExpenseType.SHARE, testUser, generateRandomDate(randomYear, randomMonth));
        shareExpense.updateOriginalExpense(groupExpense);
        shareExpense.updateGroup(testGroup);

        given(userRepository.findById(testUser.getUserId())).willReturn(Optional.of(testUser));
        given(expenseRepository.findPersonalExpensesForCombined(testUser, randomYear, randomMonth, null, null, Pageable.unpaged()))
                .willReturn(new PageImpl<>(List.of(personalExpense)));
        given(expenseRepository.findShareExpensesForCombined(testUser, randomYear, randomMonth, null, null, Pageable.unpaged()))
                .willReturn(new PageImpl<>(List.of(shareExpense)));

        PageWithSummaryResponse<CombinedExpenseResponseDTO> result = expenseService.getCombinedExpenses(testUser.getUserId(), searchDTO, pageable);

        assertThat(result.page().getTotalElements()).isEqualTo(2);
        assertThat(result.summary().myTotalShareAmount()).isEqualByComparingTo("40000");
        assertThat(result.page().getContent()).hasSize(2);
    }

    @Test
    @DisplayName("그룹 지출 목록 및 통계 조회 성공")
    void getGroupExpensesWithStats_Success() {
        ExpenseSearchDTO searchDTO = new ExpenseSearchDTO(randomYear, randomMonth, null, null, 1);
        Pageable pageable = PageRequest.of(0, 10);
        Page<Expense> expensePage = new PageImpl<>(List.of(groupExpense), pageable, 1);

        given(userRepository.findById(testUser.getUserId())).willReturn(Optional.of(testUser));
        given(groupRepository.findById(testGroup.getGroupId())).willReturn(Optional.of(testGroup));
        given(groupMemberRepository.existsByGroupAndUser(testGroup, testUser)).willReturn(true);
        given(expenseRepository.findGroupExpensesByMonthWithPaging(testGroup, randomYear, randomMonth, pageable)).willReturn(expensePage);
        given(expenseRepository.findGroupExpensesByMonthWithPaging(testGroup, randomYear, randomMonth, Pageable.unpaged())).willReturn(new PageImpl<>(List.of(groupExpense)));
        given(expenseSplitRepository.findByExpense(any())).willReturn(List.of());

        PageWithSummaryResponse<ExpenseResponseDTO> result = expenseService.getGroupExpensesWithStats(testUser.getUserId(), testGroup.getGroupId(), searchDTO, pageable);

        assertThat(result.page().getContent()).hasSize(1);
        assertThat(result.page().getContent().get(0).title()).isEqualTo("회식비");
        assertThat(result.summary().totalAmount()).isEqualByComparingTo("50000");
    }

    @Test
    @DisplayName("그룹 멤버가 아닌 경우 그룹 지출 통계 조회 실패")
    void getGroupExpensesWithStats_NotGroupMember_Fail() {
        ExpenseSearchDTO searchDTO = new ExpenseSearchDTO(randomYear, randomMonth, null, null, 1);
        Pageable pageable = PageRequest.of(0, 10);

        given(userRepository.findById(otherUser.getUserId())).willReturn(Optional.of(otherUser));
        given(groupRepository.findById(testGroup.getGroupId())).willReturn(Optional.of(testGroup));
        given(groupMemberRepository.existsByGroupAndUser(testGroup, otherUser)).willReturn(false);

        assertThatThrownBy(() -> expenseService.getGroupExpensesWithStats(otherUser.getUserId(), testGroup.getGroupId(), searchDTO, pageable))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.GROUP_MEMBER_NOT_FOUND);
    }

    @Test
    @DisplayName("영수증 업로드와 함께 지출 생성 성공")
    void createExpense_WithReceipt_Success() {
        UUID receiptFileId = UUID.randomUUID();
        ImageFile imageFile = ImageFile.builder()
                .fileId(receiptFileId)
                .originalName("test.jpg")
                .storedName("stored.jpg")
                .filePath("http://s3.url/test.jpg")
                .fileSize(100L)
                .mimeType("image/jpeg")
                .build();

        given(mockFile.isEmpty()).willReturn(false);
        given(userRepository.findById(testUser.getUserId())).willReturn(Optional.of(testUser));
        given(imageService.uploadImageToS3(any(), any(), anyInt())).willReturn(imageFile);
        given(expenseRepository.save(any(Expense.class))).willAnswer(invocation -> {
            Expense expense = invocation.getArgument(0);
            assertThat(expense.getReceiptImageFile().getFileId()).isEqualTo(receiptFileId);
            return expense;
        });

        expenseService.createPersonalExpense(testUser.getUserId(), createPersonalRequestDTO, mockFile);

        then(imageService).should(times(1)).uploadImageToS3(any(), any(), anyInt());
        then(expenseRepository).should(times(1)).save(any(Expense.class));
    }


    @Test
    @DisplayName("개인 지출 월별 추이 조회 성공")
    void getPersonalExpenseTrend_Success() {
        ExpenseSearchDTO searchDTO = new ExpenseSearchDTO(randomYear, randomMonth, null, null, 3);
        LocalDate startDate = LocalDate.of(randomYear, randomMonth - 2 > 0 ? randomMonth - 2 : 1, 1);
        // 해당 월의 마지막 날짜를 동적으로 계산
        LocalDate endDate = YearMonth.of(randomYear, randomMonth).atEndOfMonth();

        Expense expenseJan = createTestExpense(10L, "1월 식비", new BigDecimal("10000"), ExpenseType.PERSONAL, testUser, LocalDate.of(randomYear, randomMonth - 2 > 0 ? randomMonth - 2 : 1, 10));
        Expense expenseFeb = createTestExpense(12L, "2월 식비", new BigDecimal("12000"), ExpenseType.PERSONAL, testUser, LocalDate.of(randomYear, randomMonth - 1 > 0 ? randomMonth - 1 : 1, 5));

        given(userRepository.findById(testUser.getUserId())).willReturn(Optional.of(testUser));
        given(expenseRepository.findPersonalExpensesByMonthRange(testUser, startDate, endDate, null))
                .willReturn(List.of(expenseJan, expenseFeb));

        MonthlyExpenseTrendResponseDTO result = expenseService.getPersonalExpenseTrend(testUser.getUserId(), searchDTO);

        assertThat(result.monthlyData()).hasSize(3);
        assertThat(result.monthlyData().get(String.format("%d-%02d", randomYear, randomMonth - 2 > 0 ? randomMonth - 2 : 1))).hasSize(1);
        assertThat(result.monthlyData().get(String.format("%d-%02d", randomYear, randomMonth - 1 > 0 ? randomMonth - 1 : 1))).hasSize(1);
        assertThat(result.monthlyData().get(String.format("%d-%02d", randomYear, randomMonth))).isEmpty();
    }

}