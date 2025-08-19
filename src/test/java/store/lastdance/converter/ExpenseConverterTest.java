package store.lastdance.converter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import store.lastdance.domain.common.ImageFile;
import store.lastdance.domain.expense.*;
import store.lastdance.domain.group.Group;
import store.lastdance.domain.user.User;
import store.lastdance.dto.expense.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ExpenseConverterTest {

    @InjectMocks
    private ExpenseConverter expenseConverter;

    private Expense expense;
    private User user;
    private Group group;
    private ImageFile receiptImageFile;

    @BeforeEach
    void setUp() {
        user = mock(User.class);
        when(user.getUserId()).thenReturn(UUID.randomUUID());
        when(user.getNickname()).thenReturn("TestUser");

        group = mock(Group.class);
        when(group.getGroupId()).thenReturn(UUID.randomUUID());
        when(group.getGroupName()).thenReturn("TestGroup");

        receiptImageFile = mock(ImageFile.class);
        when(receiptImageFile.getFileId()).thenReturn(UUID.randomUUID());

        expense = mock(Expense.class);
        when(expense.getExpenseId()).thenReturn(1L);
        when(expense.getTitle()).thenReturn("Test Expense");
        when(expense.getAmount()).thenReturn(BigDecimal.valueOf(100.0));
        when(expense.getCategory()).thenReturn(ExpenseCategory.FOOD);
        when(expense.getExpenseType()).thenReturn(ExpenseType.PERSONAL);
        when(expense.getSplitType()).thenReturn(SplitType.EQUAL);
        when(expense.getExpenseDate()).thenReturn(LocalDate.now());
        when(expense.getMemo()).thenReturn("Test Memo");
        when(expense.getCreatedAt()).thenReturn(LocalDateTime.now());
        when(expense.getUpdatedAt()).thenReturn(LocalDateTime.now());
        when(expense.getUser()).thenReturn(user);
        when(expense.getGroup()).thenReturn(group);
        when(expense.getReceiptImageFile()).thenReturn(receiptImageFile);
    }

    @Test
    @DisplayName("Expense를 ExpenseResponseDTO로 변환 - 기본")
    void toResponseDTO_basic() {
        // When
        ExpenseResponseDTO dto = expenseConverter.toResponseDTO(expense);

        // Then
        assertThat(dto).isNotNull();
        assertThat(dto.expenseId()).isEqualTo(expense.getExpenseId());
        assertThat(dto.title()).isEqualTo(expense.getTitle());
        assertThat(dto.amount()).isEqualTo(expense.getAmount());
        assertThat(dto.category()).isEqualTo(expense.getCategory());
        assertThat(dto.expenseType()).isEqualTo(expense.getExpenseType());
        assertThat(dto.splitType()).isEqualTo(expense.getSplitType());
        assertThat(dto.splitData()).isNull();
        assertThat(dto.date()).isEqualTo(expense.getExpenseDate());
        assertThat(dto.memo()).isEqualTo(expense.getMemo());
        assertThat(dto.groupId()).isEqualTo(group.getGroupId());
        assertThat(dto.userId()).isEqualTo(user.getUserId());
        assertThat(dto.createdAt()).isEqualTo(expense.getCreatedAt());
        assertThat(dto.updatedAt()).isEqualTo(expense.getUpdatedAt());
        assertThat(dto.receiptImageFileId()).isEqualTo(receiptImageFile.getFileId());
        assertThat(dto.hasReceipt()).isTrue();
    }

    @Test
    @DisplayName("Expense가 null일 때 ExpenseResponseDTO로 변환 - null 반환")
    void toResponseDTO_nullExpense() {
        // When
        ExpenseResponseDTO dto = expenseConverter.toResponseDTO(null);

        // Then
        assertThat(dto).isNull();
    }

    @Test
    @DisplayName("Expense와 SplitData를 ExpenseResponseDTO로 변환")
    void toResponseDTO_withSplitData() {
        // Given
        List<SplitDataDTO> splitData = Arrays.asList(
                new SplitDataDTO(user.getUserId(), BigDecimal.valueOf(50.0)),
                new SplitDataDTO(UUID.randomUUID(), BigDecimal.valueOf(50.0))
        );

        // When
        ExpenseResponseDTO dto = expenseConverter.toResponseDTO(expense, splitData);

        // Then
        assertThat(dto).isNotNull();
        assertThat(dto.splitData()).isEqualTo(splitData);
        assertThat(dto.splitData()).hasSize(2);
    }

    @Test
    @DisplayName("Expense를 CombinedExpenseResponseDTO로 변환 - 개인 경비")
    void toCombinedResponseDTO_personalExpense() {
        // When
        CombinedExpenseResponseDTO dto = expenseConverter.toCombinedResponseDTO(expense);

        // Then
        assertThat(dto).isNotNull();
        assertThat(dto.expenseId()).isEqualTo(expense.getExpenseId());
        assertThat(dto.originalExpenseId()).isNull();
        assertThat(dto.expenseType()).isEqualTo("PERSONAL");
        assertThat(dto.title()).isEqualTo(expense.getTitle());
        assertThat(dto.amount()).isEqualTo(expense.getAmount());
        assertThat(dto.myShareAmount()).isEqualTo(expense.getAmount());
        assertThat(dto.category()).isEqualTo(expense.getCategory());
        assertThat(dto.date()).isEqualTo(expense.getExpenseDate().atStartOfDay());
        assertThat(dto.memo()).isEqualTo(expense.getMemo());
        assertThat(dto.hasReceipt()).isTrue();
        assertThat(dto.groupId()).isNull();
        assertThat(dto.groupName()).isNull();
    }

    @Test
    @DisplayName("Expense가 null일 때 CombinedExpenseResponseDTO로 변환 - null 반환")
    void toCombinedResponseDTO_nullExpense() {
        // When
        CombinedExpenseResponseDTO dto = expenseConverter.toCombinedResponseDTO(null);

        // Then
        assertThat(dto).isNull();
    }

    @Test
    @DisplayName("공유 경비를 CombinedExpenseResponseDTO로 변환")
    void toCombinedResponseDTO_shareExpense() {
        // Given
        Expense shareExpense = mock(Expense.class);
        when(shareExpense.getExpenseId()).thenReturn(2L);
        when(shareExpense.getTitle()).thenReturn("Share Expense");
        when(shareExpense.getAmount()).thenReturn(BigDecimal.valueOf(50.0));
        when(shareExpense.getCategory()).thenReturn(ExpenseCategory.FOOD); // Changed from TRANSPORTATION
        when(shareExpense.getExpenseDate()).thenReturn(LocalDate.now());
        when(shareExpense.getMemo()).thenReturn("Share Memo");
        when(shareExpense.getGroup()).thenReturn(group);
        when(shareExpense.getOriginalExpense()).thenReturn(expense); // Mock original expense

        String groupName = "TestGroup";

        // When
        CombinedExpenseResponseDTO dto = expenseConverter.toCombinedResponseDTO(shareExpense, expense, groupName);

        // Then
        assertThat(dto).isNotNull();
        assertThat(dto.expenseId()).isEqualTo(shareExpense.getExpenseId());
        assertThat(dto.originalExpenseId()).isEqualTo(expense.getExpenseId());
        assertThat(dto.expenseType()).isEqualTo("SHARE");
        assertThat(dto.title()).isEqualTo(shareExpense.getTitle());
        assertThat(dto.amount()).isEqualTo(expense.getAmount());
        assertThat(dto.myShareAmount()).isEqualTo(shareExpense.getAmount());
        assertThat(dto.category()).isEqualTo(shareExpense.getCategory());
        assertThat(dto.date()).isEqualTo(shareExpense.getExpenseDate().atStartOfDay());
        assertThat(dto.memo()).isEqualTo(shareExpense.getMemo());
        assertThat(dto.hasReceipt()).isTrue();
        assertThat(dto.groupId()).isEqualTo(group.getGroupId());
        assertThat(dto.groupName()).isEqualTo(groupName);
    }

    @Test
    @DisplayName("공유 경비 변환 시 shareExpense가 null일 때 CombinedExpenseResponseDTO로 변환 - null 반환")
    void toCombinedResponseDTO_shareExpense_nullShareExpense() {
        // When
        CombinedExpenseResponseDTO dto = expenseConverter.toCombinedResponseDTO(null, expense, "TestGroup");

        // Then
        assertThat(dto).isNull();
    }

    @Test
    @DisplayName("공유 경비 변환 시 originalExpense가 null일 때 CombinedExpenseResponseDTO로 ���환 - shareExpense 값 사용")
    void toCombinedResponseDTO_shareExpense_nullOriginalExpense() {
        // Given
        Expense shareExpense = mock(Expense.class);
        when(shareExpense.getExpenseId()).thenReturn(2L);
        when(shareExpense.getTitle()).thenReturn("Share Expense");
        when(shareExpense.getAmount()).thenReturn(BigDecimal.valueOf(50.0));
        when(shareExpense.getCategory()).thenReturn(ExpenseCategory.FOOD);
        when(shareExpense.getExpenseDate()).thenReturn(LocalDate.now());
        when(shareExpense.getMemo()).thenReturn("Share Memo");
        when(shareExpense.getGroup()).thenReturn(group);
        when(shareExpense.getReceiptImageFile()).thenReturn(null);

        String groupName = "TestGroup";

        // When
        CombinedExpenseResponseDTO dto = expenseConverter.toCombinedResponseDTO(shareExpense, null, groupName);

        // Then
        assertThat(dto).isNotNull();
        assertThat(dto.expenseId()).isEqualTo(shareExpense.getExpenseId());
        assertThat(dto.originalExpenseId()).isNull();
        assertThat(dto.expenseType()).isEqualTo("SHARE");
        assertThat(dto.title()).isEqualTo(shareExpense.getTitle());
        assertThat(dto.amount()).isEqualTo(shareExpense.getAmount());
        assertThat(dto.myShareAmount()).isEqualTo(shareExpense.getAmount());
        assertThat(dto.category()).isEqualTo(shareExpense.getCategory());
        assertThat(dto.date()).isEqualTo(shareExpense.getExpenseDate().atStartOfDay());
        assertThat(dto.memo()).isEqualTo(shareExpense.getMemo());
        assertThat(dto.hasReceipt()).isFalse();
        assertThat(dto.groupId()).isEqualTo(group.getGroupId());
        assertThat(dto.groupName()).isEqualTo(groupName);
    }

    @Test
    @DisplayName("그룹 공유 경비를 GroupShareExpenseResponseDTO로 변환")
    void toGroupShareExpenseResponseDTO() {
        // Given
        Expense shareExpense = mock(Expense.class);
        when(shareExpense.getExpenseId()).thenReturn(2L);
        when(shareExpense.getTitle()).thenReturn("Group Share Expense");
        when(shareExpense.getAmount()).thenReturn(BigDecimal.valueOf(30.0));
        when(shareExpense.getCategory()).thenReturn(ExpenseCategory.FOOD); // Changed from LEISURE
        when(shareExpense.getExpenseDate()).thenReturn(LocalDate.now());
        when(shareExpense.getMemo()).thenReturn("Group Share Memo");
        when(shareExpense.getGroup()).thenReturn(group);
        when(shareExpense.getSplitType()).thenReturn(SplitType.EQUAL); // Changed from N_WAY

        List<SplitDataDTO> splitData = Arrays.asList(
                new SplitDataDTO(user.getUserId(), BigDecimal.valueOf(30.0))
        );
        String groupName = "TestGroup";

        // When
        GroupShareExpenseResponseDTO dto = expenseConverter.toGroupShareExpenseResponseDTO(shareExpense, expense, groupName, splitData);

        // Then
        assertThat(dto).isNotNull();
        assertThat(dto.expenseId()).isEqualTo(shareExpense.getExpenseId());
        assertThat(dto.title()).isEqualTo(shareExpense.getTitle());
        assertThat(dto.amount()).isEqualTo(expense.getAmount()); // This should be original expense amount
        assertThat(dto.myShareAmount()).isEqualTo(shareExpense.getAmount());
        assertThat(dto.category()).isEqualTo(shareExpense.getCategory());
        assertThat(dto.date()).isEqualTo(shareExpense.getExpenseDate());
        assertThat(dto.memo()).isEqualTo(shareExpense.getMemo());
        assertThat(dto.groupId()).isEqualTo(group.getGroupId());
        assertThat(dto.groupName()).isEqualTo(groupName);
        assertThat(dto.splitType()).isEqualTo(expense.getSplitType());
        assertThat(dto.splitData()).isEqualTo(splitData);
        assertThat(dto.receiptImageFileId()).isEqualTo(receiptImageFile.getFileId());
        assertThat(dto.hasReceipt()).isTrue();
        assertThat(dto.originalExpenseId()).isEqualTo(expense.getExpenseId());
    }

    @Test
    @DisplayName("그룹 공유 경비 변환 시 shareExpense가 null일 때 GroupShareExpenseResponseDTO로 변환 - null 반환")
    void toGroupShareExpenseResponseDTO_nullShareExpense() {
        // Given
        List<SplitDataDTO> splitData = Arrays.asList();
        String groupName = "TestGroup";

        // When
        GroupShareExpenseResponseDTO dto = expenseConverter.toGroupShareExpenseResponseDTO(null, expense, groupName, splitData);

        // Then
        assertThat(dto).isNull();
    }

    @Test
    @DisplayName("그룹 공유 경비 변환 시 originalExpense가 null일 때 GroupShareExpenseResponseDTO로 변환 - shareExpense의 값 사용")
    void toGroupShareExpenseResponseDTO_nullOriginalExpense() {
        // Given
        Expense shareExpense = mock(Expense.class);
        when(shareExpense.getExpenseId()).thenReturn(2L);
        when(shareExpense.getTitle()).thenReturn("Group Share Expense");
        when(shareExpense.getAmount()).thenReturn(BigDecimal.valueOf(30.0));
        when(shareExpense.getCategory()).thenReturn(ExpenseCategory.FOOD); // Changed from LEISURE
        when(shareExpense.getExpenseDate()).thenReturn(LocalDate.now());
        when(shareExpense.getMemo()).thenReturn("Group Share Memo");
        when(shareExpense.getGroup()).thenReturn(group);
        when(shareExpense.getSplitType()).thenReturn(SplitType.EQUAL); // Changed from N_WAY

        List<SplitDataDTO> splitData = Arrays.asList(
                new SplitDataDTO(user.getUserId(), BigDecimal.valueOf(30.0))
        );
        String groupName = "TestGroup";

        // When
        GroupShareExpenseResponseDTO dto = expenseConverter.toGroupShareExpenseResponseDTO(shareExpense, null, groupName, splitData);

        // Then
        assertThat(dto).isNotNull();
        assertThat(dto.amount()).isEqualTo(shareExpense.getAmount());
        assertThat(dto.splitType()).isEqualTo(shareExpense.getSplitType());
        assertThat(dto.receiptImageFileId()).isNull();
        assertThat(dto.hasReceipt()).isFalse();
        assertThat(dto.originalExpenseId()).isNull();
    }

    @Test
    @DisplayName("그룹 통합 경비를 GroupCombinedExpenseResponseDTO로 변환")
    void toGroupCombinedResponseDTO() {
        // Given
        BigDecimal myShareAmount = BigDecimal.valueOf(25.0);
        String groupName = "TestGroup";
        User creator = user;

        User participant1 = mock(User.class);
        when(participant1.getUserId()).thenReturn(UUID.randomUUID());
        when(participant1.getNickname()).thenReturn("Participant1");

        User participant2 = mock(User.class);
        when(participant2.getUserId()).thenReturn(UUID.randomUUID());
        when(participant2.getNickname()).thenReturn("Participant2");

        List<User> participants = Arrays.asList(participant1, participant2);

        ExpenseSplit split1 = mock(ExpenseSplit.class);
        when(split1.getUser()).thenReturn(participant1);
        when(split1.getAmount()).thenReturn(BigDecimal.valueOf(50.0));

        ExpenseSplit split2 = mock(ExpenseSplit.class);
        when(split2.getUser()).thenReturn(participant2);
        when(split2.getAmount()).thenReturn(BigDecimal.valueOf(50.0));

        List<ExpenseSplit> splits = Arrays.asList(split1, split2);

        // When
        GroupCombinedExpenseResponseDTO dto = expenseConverter.toGroupCombinedResponseDTO(
                expense, myShareAmount, groupName, creator, splits, participants);

        // Then
        assertThat(dto).isNotNull();
        assertThat(dto.expenseId()).isEqualTo(expense.getExpenseId());
        assertThat(dto.expenseType()).isEqualTo("GROUP");
        assertThat(dto.title()).isEqualTo(expense.getTitle());
        assertThat(dto.amount()).isEqualTo(expense.getAmount());
        assertThat(dto.myShareAmount()).isEqualTo(myShareAmount);
        assertThat(dto.category()).isEqualTo(expense.getCategory());
        assertThat(dto.date()).isEqualTo(expense.getExpenseDate());
        assertThat(dto.memo()).isEqualTo(expense.getMemo());
        assertThat(dto.hasReceipt()).isTrue();
        assertThat(dto.groupId()).isEqualTo(group.getGroupId());
        assertThat(dto.groupName()).isEqualTo(groupName);
        assertThat(dto.createdBy()).isEqualTo(creator.getUserId());
        assertThat(dto.createdByName()).isEqualTo(creator.getNickname());
        assertThat(dto.splitType()).isEqualTo(expense.getSplitType());
        assertThat(dto.participants()).hasSize(2);
        assertThat(dto.participants().get(0).userId()).isEqualTo(participant1.getUserId());
        assertThat(dto.participants().get(0).userName()).isEqualTo(participant1.getNickname());
        assertThat(dto.participants().get(0).shareAmount()).isEqualTo(BigDecimal.valueOf(50.0));
        assertThat(dto.participants().get(1).userId()).isEqualTo(participant2.getUserId());
        assertThat(dto.participants().get(1).userName()).isEqualTo(participant2.getNickname());
        assertThat(dto.participants().get(1).shareAmount()).isEqualTo(BigDecimal.valueOf(50.0));
    }

    @Test
    @DisplayName("GroupCombinedExpenseResponseDTO 변환 시 expense가 null일 때 - null 반환")
    void toGroupCombinedResponseDTO_nullExpense() {
        // Given
        BigDecimal myShareAmount = BigDecimal.valueOf(25.0);
        String groupName = "TestGroup";
        User creator = user;
        List<ExpenseSplit> splits = Arrays.asList();
        List<User> participants = Arrays.asList();

        // When
        GroupCombinedExpenseResponseDTO dto = expenseConverter.toGroupCombinedResponseDTO(
                null, myShareAmount, groupName, creator, splits, participants);

        // Then
        assertThat(dto).isNull();
    }

    @Test
    @DisplayName("월별 경비 트렌드를 MonthlyExpenseTrendResponseDTO로 변환")
    void toMonthlyTrendResponseDTO() {
        // Given
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 1, 31);

        ExpenseResponseDTO expense1 = mock(ExpenseResponseDTO.class);
        ExpenseResponseDTO expense2 = mock(ExpenseResponseDTO.class);

        Map<String, List<ExpenseResponseDTO>> monthlyData = Collections.singletonMap(
                "2024-01", Arrays.asList(expense1, expense2)
        );

        // When
        MonthlyExpenseTrendResponseDTO dto = expenseConverter.toMonthlyTrendResponseDTO(monthlyData, startDate, endDate);

        // Then
        assertThat(dto).isNotNull();
        assertThat(dto.monthlyData()).isEqualTo(monthlyData);
        assertThat(dto.totalCount()).isEqualTo(2);
        assertThat(dto.dateRange().start()).isEqualTo(startDate.atStartOfDay());
        assertThat(dto.dateRange().end()).isEqualTo(endDate.atTime(23, 59, 59));
    }

    @Test
    @DisplayName("MonthlyExpenseTrendResponseDTO 변환 시 빈 데이터로 변환")
    void toMonthlyTrendResponseDTO_emptyData() {
        // Given
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 1, 31);
        Map<String, List<ExpenseResponseDTO>> monthlyData = Collections.emptyMap();

        // When
        MonthlyExpenseTrendResponseDTO dto = expenseConverter.toMonthlyTrendResponseDTO(monthlyData, startDate, endDate);

        // Then
        assertThat(dto).isNotNull();
        assertThat(dto.monthlyData()).isEmpty();
        assertThat(dto.totalCount()).isEqualTo(0);
    }
}
