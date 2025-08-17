package store.lastdance.service.expense;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
import store.lastdance.domain.common.ImageFile;
import store.lastdance.domain.expense.Expense;
import store.lastdance.domain.expense.ExpenseAnalysisHistory;
import store.lastdance.domain.expense.ExpenseCategory;
import store.lastdance.domain.expense.ExpenseType;
import store.lastdance.domain.group.Group;
import store.lastdance.domain.user.OAuthProvider;
import store.lastdance.domain.user.User;
import store.lastdance.domain.user.UserRole;
import store.lastdance.dto.expense.ExpenseResponseDTO;
import store.lastdance.dto.expense.ExpenseSearchDTO;
import store.lastdance.dto.expense.GroupShareExpenseResponseDTO;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ExpenseV2QueryServiceImplTest {

    @InjectMocks
    private ExpenseV2QueryServiceImpl expenseV2QueryService;

    @Mock
    private ExpenseRepository expenseRepository;
    @Mock
    private ExpenseSplitRepository expenseSplitRepository;
    @Mock
    private GroupRepository groupRepository;
    @Mock
    private GroupMemberRepository groupMemberRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ExpenseAnalysisHistoryRepository expenseAnalysisHistoryRepository;
    @Mock
    private ImageService imageService;

    private User user;
    private Group group;
    private Expense personalExpense;
    private Expense groupExpense;
    private Expense shareExpense;


    private User createTestUser(String nickname) {
        User testUser = User.builder()
                .email(nickname + "@test.com")
                .username(nickname)
                .nickname(nickname)
                .provider(OAuthProvider.KAKAO)
                .providerId("providerId_" + nickname)
                .role(UserRole.USER)
                .build();
        ReflectionTestUtils.setField(testUser, "userId", UUID.randomUUID());
        return testUser;
    }

    @BeforeEach
    void setUp() {
        user = createTestUser("testUser");
        group = Group.builder().groupName("testGroup").owner(user).inviteCode("123456").build();
        ReflectionTestUtils.setField(group, "groupId", UUID.randomUUID());

        personalExpense = Expense.builder()
                .title("개인 지출")
                .amount(new BigDecimal("10000"))
                .category(ExpenseCategory.FOOD)
                .expenseType(ExpenseType.PERSONAL)
                .user(user)
                .expenseDate(LocalDate.now())
                .build();
        ReflectionTestUtils.setField(personalExpense, "expenseId", 1L);

        groupExpense = Expense.builder()
                .title("그룹 지출")
                .amount(new BigDecimal("30000"))
                .category(ExpenseCategory.TRANSPORT)
                .expenseType(ExpenseType.GROUP)
                .user(user)
                .expenseDate(LocalDate.now())
                .build();
        groupExpense.updateGroup(group);
        ReflectionTestUtils.setField(groupExpense, "expenseId", 2L);

        shareExpense = Expense.builder()
                .title("분담 지출")
                .amount(new BigDecimal("15000"))
                .category(ExpenseCategory.TRANSPORT)
                .expenseType(ExpenseType.SHARE)
                .user(user)
                .expenseDate(LocalDate.now())
                .build();
        shareExpense.updateGroup(group);
        shareExpense.updateOriginalExpense(groupExpense);
        ReflectionTestUtils.setField(shareExpense, "expenseId", 3L);
    }

    @Nested
    @DisplayName("지출 단건 조회 (getExpenseById)")
    class GetExpenseById {

        @Test
        @DisplayName("성공 - 개인 지출 조회")
        void getPersonalExpense_Success() {
            // given
            given(userRepository.findById(user.getUserId())).willReturn(Optional.of(user));
            given(expenseRepository.findByExpenseIdWithPermission(personalExpense.getExpenseId(), user)).willReturn(Optional.of(personalExpense));

            // when
            ExpenseResponseDTO responseDTO = expenseV2QueryService.getExpenseById(user.getUserId(), personalExpense.getExpenseId());

            // then
            assertThat(responseDTO.expenseId()).isEqualTo(personalExpense.getExpenseId());
            assertThat(responseDTO.title()).isEqualTo(personalExpense.getTitle());
            assertThat(responseDTO.expenseType()).isEqualTo(ExpenseType.PERSONAL);
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 지출")
        void getExpense_NotFound_Fail() {
            // given
            Long nonExistExpenseId = 999L;
            given(userRepository.findById(user.getUserId())).willReturn(Optional.of(user));
            given(expenseRepository.findByExpenseIdWithPermission(nonExistExpenseId, user)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> expenseV2QueryService.getExpenseById(user.getUserId(), nonExistExpenseId))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EXPENSE_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("그룹 분담금 조회 (getGroupShareExpenses)")
    class GetGroupShareExpenses {
        @Test
        @DisplayName("성공")
        void getGroupShareExpenses_Success() {
            // given
            int year = LocalDate.now().getYear();
            int month = LocalDate.now().getMonthValue();
            ExpenseSearchDTO searchDTO = new ExpenseSearchDTO(year, month, null, null, null);

            given(userRepository.findById(user.getUserId())).willReturn(Optional.of(user));
            given(expenseRepository.findShareExpensesByUserAndMonth(user, year, month)).willReturn(List.of(shareExpense));

            // when
            List<GroupShareExpenseResponseDTO> result = expenseV2QueryService.getGroupShareExpenses(user.getUserId(), searchDTO);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).expenseId()).isEqualTo(shareExpense.getExpenseId());
            assertThat(result.get(0).originalExpenseId()).isEqualTo(groupExpense.getExpenseId());
        }
    }

    @Nested
    @DisplayName("영수증 URL 조회 (getReceiptImageUrl)")
    class GetReceiptImageUrl {
        @Test
        @DisplayName("성공")
        void getReceiptImageUrl_Success() {
            // given
            UUID fileId = UUID.randomUUID();
            ImageFile imageFile = ImageFile.builder()
                    .fileId(fileId)
                    .originalName("receipt.jpg")
                    .storedName("random-uuid-receipt.jpg")
                    .filePath("images/random-uuid-receipt.jpg")
                    .fileSize(1024L)
                    .mimeType("image/jpeg")
                    .build();
            personalExpense.updateReceiptImageFile(imageFile);
            String presignedUrl = "https://s3.test.com/receipt.jpg";

            given(userRepository.findById(user.getUserId())).willReturn(Optional.of(user));
            given(expenseRepository.findByExpenseIdWithPermission(personalExpense.getExpenseId(), user)).willReturn(Optional.of(personalExpense));
            given(imageService.generatePresignedUrl(fileId)).willReturn(presignedUrl);

            // when
            String resultUrl = expenseV2QueryService.getReceiptImageUrl(personalExpense.getExpenseId(), user.getUserId());

            // then
            assertThat(resultUrl).isEqualTo(presignedUrl);
        }

        @Test
        @DisplayName("성공 - 영수증 없는 경우 null 반환")
        void getReceiptImageUrl_NoReceipt_ReturnNull() {
            // given
            personalExpense.updateReceiptImageFile(null);
            given(userRepository.findById(user.getUserId())).willReturn(Optional.of(user));
            given(expenseRepository.findByExpenseIdWithPermission(personalExpense.getExpenseId(), user)).willReturn(Optional.of(personalExpense));

            // when
            String resultUrl = expenseV2QueryService.getReceiptImageUrl(personalExpense.getExpenseId(), user.getUserId());

            // then
            assertThat(resultUrl).isNull();
        }
    }

    @Nested
    @DisplayName("그룹 분담 지출 통계 조회 (getGroupShareExpensesWithPaging)")
    class GetGroupShareExpensesWithPaging {
        @Test
        @DisplayName("성공")
        void getGroupShareExpensesWithPaging_Success() {
            // given
            int year = LocalDate.now().getYear();
            int month = LocalDate.now().getMonthValue();
            ExpenseSearchDTO searchDTO = new ExpenseSearchDTO(year, month, null, null, null);
            Pageable pageable = PageRequest.of(0, 10);
            Page<Expense> pagedExpenses = new PageImpl<>(List.of(shareExpense), pageable, 1);

            given(userRepository.findById(user.getUserId())).willReturn(Optional.of(user));
            given(groupRepository.findById(group.getGroupId())).willReturn(Optional.of(group));
            given(expenseRepository.findShareExpensesByGroupAndMonthWithPagingFiltered(user, group, year, month, null, null, pageable))
                    .willReturn(pagedExpenses);
            given(expenseRepository.findShareExpensesByGroupAndMonthWithPagingFiltered(user, group, year, month, null, null, Pageable.unpaged()))
                    .willReturn(new PageImpl<>(List.of(shareExpense)));

            // when
            PageWithSummaryResponse<GroupShareExpenseResponseDTO> result = expenseV2QueryService.getGroupShareExpensesWithPaging(user.getUserId(), group.getGroupId(), searchDTO, pageable);

            // then
            assertThat(result.page().getTotalElements()).isEqualTo(1);
            assertThat(result.page().getContent().get(0).expenseId()).isEqualTo(shareExpense.getExpenseId());
            assertThat(result.summary().totalAmount()).isEqualByComparingTo("30000"); // 원본 지출 금액
            assertThat(result.summary().myTotalShareAmount()).isEqualByComparingTo("15000"); // 분담 지출 금액
        }
    }

    @Nested
    @DisplayName("LLM 지출 분석 기록 조회 (getExpenseAnalysisHistory)")
    class GetExpenseAnalysisHistory {
        @Test
        @DisplayName("성공")
        void getExpenseAnalysisHistory_Success() {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            ExpenseAnalysisHistory history = ExpenseAnalysisHistory.builder()
                    .user(user)
                    .startDate(LocalDate.now().withDayOfMonth(1))
                    .endDate(LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth()))
                    .budgetUsagePercentage(50.0)
                    .budgetUsageCurrentSpending(new BigDecimal("500000"))
                    .budgetUsageTotalBudget(new BigDecimal("1000000"))
                    .dailySpendingAverageSoFar(new BigDecimal("16666"))
                    .dailySpendingEstimatedEom(new BigDecimal("516646"))
                    .mainFinding("주요 발견")
                    .suggestionTitle("제안 제목")
                    .suggestionDescription("제안 설명")
                    .suggestionEffect("기대 효과")
                    .suggestionDifficulty("난이도")
                    .build();
            Page<ExpenseAnalysisHistory> pagedHistory = new PageImpl<>(List.of(history), pageable, 1);

            given(userRepository.findById(user.getUserId())).willReturn(Optional.of(user));
            given(expenseAnalysisHistoryRepository.findByUser(user, pageable)).willReturn(pagedHistory);

            // when
            PageWithSummaryResponse<?> result = expenseV2QueryService.getExpenseAnalysisHistory(user.getUserId(), pageable);

            // then
            assertThat(result.page().getTotalElements()).isEqualTo(1);
            verify(expenseAnalysisHistoryRepository).findByUser(user, pageable);
        }
    }
}
