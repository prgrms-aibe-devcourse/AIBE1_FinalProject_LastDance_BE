package store.lastdance.service.expense;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;
import store.lastdance.converter.expense.ExpenseConverter;
import store.lastdance.domain.expense.*;
import store.lastdance.domain.group.Group;
import store.lastdance.domain.group.GroupMember;
import store.lastdance.domain.user.OAuthProvider;
import store.lastdance.domain.user.User;
import store.lastdance.domain.user.UserRole;
import store.lastdance.dto.expense.*;
import store.lastdance.repository.expense.ExpenseRepository;
import store.lastdance.repository.expense.ExpenseSplitRepository;
import store.lastdance.repository.group.GroupMemberRepository;
import store.lastdance.repository.group.GroupRepository;
import store.lastdance.repository.user.UserRepository;
import store.lastdance.service.image.ImageService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ExpenseV2ServiceImplTest {

    @InjectMocks
    private ExpenseV2ServiceImpl expenseV2Service;

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
    private ImageService imageService;
    @Mock
    private ExpenseSplitter expenseSplitter;
    @Mock
    private ExpenseConverter expenseConverter;

    private User user;
    private Group group;
    private List<User> groupUsers;
    private List<GroupMember> groupMembers;

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
        groupUsers = List.of(
                createTestUser("user1"),
                createTestUser("user2"),
                createTestUser("user3")
        );

        group = Group.builder()
                .groupName("testGroup")
                .inviteCode("123456")
                .owner(user)
                .build();
        ReflectionTestUtils.setField(group, "groupId", UUID.randomUUID());

        groupMembers = groupUsers.stream()
                .map(u -> GroupMember.builder().user(u).group(group).build())
                .collect(Collectors.toList());
    }

    @Nested
    @DisplayName("개인 지출 생성 테스트")
    class CreatePersonalExpense {
        @Test
        @DisplayName("성공 - 영수증 파일 없음")
        void createPersonalExpense_Success_NoReceipt() {
            // given
            CreatePersonalExpenseRequestDTO requestDTO = new CreatePersonalExpenseRequestDTO(
                    "점심 식사", new BigDecimal("12000"), ExpenseCategory.FOOD, LocalDate.now(), "메모"
            );
            given(userRepository.findById(user.getUserId())).willReturn(Optional.of(user));
            given(expenseRepository.save(any(Expense.class))).willAnswer(invocation -> invocation.getArgument(0));
            given(expenseConverter.toResponseDTO(any(Expense.class))).willReturn(mock(ExpenseResponseDTO.class));

            // when
            expenseV2Service.createPersonalExpense(user.getUserId(), requestDTO, null);

            // then
            ArgumentCaptor<Expense> expenseCaptor = ArgumentCaptor.forClass(Expense.class);
            verify(expenseRepository).save(expenseCaptor.capture());
            Expense savedExpense = expenseCaptor.getValue();

            assertThat(savedExpense.getTitle()).isEqualTo(requestDTO.title());
            assertThat(savedExpense.getAmount()).isEqualByComparingTo(requestDTO.amount());
            assertThat(savedExpense.getExpenseType()).isEqualTo(ExpenseType.PERSONAL);
            assertThat(savedExpense.getUser()).isEqualTo(user);
            assertThat(savedExpense.getReceiptImageFile()).isNull();
            verifyNoInteractions(imageService);
        }
    }

    @Nested
    @DisplayName("그룹 지출 생성 테스트")
    class CreateGroupExpense {
        @Test
        @DisplayName("성공 - 분할 계산 로직 호출 및 결과 저장 검증")
        void createGroupExpense_Success() {
            // given
            BigDecimal totalAmount = new BigDecimal("10000");
            CreateGroupExpenseRequestDTO requestDTO = new CreateGroupExpenseRequestDTO(
                    "Test Expense", totalAmount, ExpenseCategory.FOOD, LocalDate.now(), "Memo",
                    group.getGroupId(), SplitType.EQUAL, null
            );

            Map<UUID, BigDecimal> splitResult = Map.of(
                    groupUsers.get(0).getUserId(), new BigDecimal("3334"),
                    groupUsers.get(1).getUserId(), new BigDecimal("3333"),
                    groupUsers.get(2).getUserId(), new BigDecimal("3333")
            );
            given(groupMemberRepository.findByGroup(any(Group.class))).willReturn(groupMembers);
            given(userRepository.findById(user.getUserId())).willReturn(Optional.of(user));
            given(groupRepository.findById(group.getGroupId())).willReturn(Optional.of(group));
            given(groupMemberRepository.existsByGroupAndUser(any(Group.class), any(User.class))).willReturn(true);
            given(expenseSplitter.split(eq(SplitType.EQUAL), eq(totalAmount), anyList(), eq(null))).willReturn(splitResult);
            given(userRepository.findAllById(anyList())).willReturn(groupUsers);
            given(expenseRepository.save(any(Expense.class))).willAnswer(inv -> {
                Expense expense = inv.getArgument(0);
                ReflectionTestUtils.setField(expense, "expenseId", System.nanoTime());
                return expense;
            });
            given(expenseConverter.toResponseDTO(any(Expense.class))).willReturn(mock(ExpenseResponseDTO.class));

            // when
            expenseV2Service.createGroupExpense(user.getUserId(), requestDTO, null);

            // then
            // 1. ExpenseSplitter.split이 올바른 인자로 호출되었는지 검증
            verify(expenseSplitter).split(eq(SplitType.EQUAL), eq(totalAmount), anyList(), eq(null));

            // 2. saveAll이 올바른 데이터와 함께 호출되었는지 검증 (수정된 부분)
            ArgumentCaptor<List<ExpenseSplit>> splitListCaptor = ArgumentCaptor.forClass(List.class);
            verify(expenseSplitRepository).saveAll(splitListCaptor.capture());
            List<ExpenseSplit> savedSplits = splitListCaptor.getValue();
            assertThat(savedSplits).hasSize(3);
            assertThat(savedSplits).anyMatch(s -> s.getAmount().compareTo(new BigDecimal("3334")) == 0);

            // 3. 원본 지출(GROUP) 저장과 분담 지출(SHARE) 저장이 분리되어 호출되는지 검증 (수정된 부분)
            ArgumentCaptor<Expense> groupExpenseCaptor = ArgumentCaptor.forClass(Expense.class);
            verify(expenseRepository).save(groupExpenseCaptor.capture());
            assertThat(groupExpenseCaptor.getValue().getExpenseType()).isEqualTo(ExpenseType.GROUP);

            ArgumentCaptor<List<Expense>> shareExpenseListCaptor = ArgumentCaptor.forClass(List.class);
            verify(expenseRepository).saveAll(shareExpenseListCaptor.capture());
            List<Expense> savedShareExpenses = shareExpenseListCaptor.getValue();
            assertThat(savedShareExpenses).hasSize(3);
            assertThat(savedShareExpenses).allMatch(e -> e.getExpenseType() == ExpenseType.SHARE);
        }
    }

    @Nested
    @DisplayName("지출 수정 테스트")
    class UpdateExpense {
        @Test
        @DisplayName("성공 - 그룹 지출 분담 내역 변경")
        void updateGroupExpense_ChangeSplit_Success() {
            // given
            Long expenseId = 1L;
            Expense existingExpense = Expense.builder()
                    .title("Original Title")
                    .amount(new BigDecimal("10000"))
                    .category(ExpenseCategory.FOOD)
                    .expenseType(ExpenseType.GROUP)
                    .user(user)
                    .expenseDate(LocalDate.now())
                    .build();
            existingExpense.updateSplitType(SplitType.EQUAL);
            existingExpense.updateGroup(group);
            ReflectionTestUtils.setField(existingExpense, "expenseId", expenseId);

            List<User> twoMembers = groupUsers.subList(0, 2);
            List<SplitDataDTO> newSplitData = List.of(
                    new SplitDataDTO(twoMembers.get(0).getUserId(), new BigDecimal("6000")),
                    new SplitDataDTO(twoMembers.get(1).getUserId(), new BigDecimal("4000"))
            );
            UpdateExpenseRequestDTO requestDTO = new UpdateExpenseRequestDTO(
                    "Updated Title", new BigDecimal("10000"), ExpenseCategory.OTHER, LocalDate.now(), "Updated Memo",
                    newSplitData, SplitType.CUSTOM
            );

            Map<UUID, BigDecimal> splitResult = Map.of(
                    twoMembers.get(0).getUserId(), new BigDecimal("6000"),
                    twoMembers.get(1).getUserId(), new BigDecimal("4000")
            );

            given(userRepository.findById(user.getUserId())).willReturn(Optional.of(user));
            given(expenseRepository.findByExpenseIdWithPermission(expenseId, user)).willReturn(Optional.of(existingExpense));
            given(groupMemberRepository.findByGroup(group)).willReturn(groupMembers);
            given(expenseSplitter.split(eq(SplitType.CUSTOM), eq(requestDTO.amount()), anyList(), eq(newSplitData))).willReturn(splitResult);
            given(userRepository.findAllById(anyList())).willReturn(twoMembers);
            given(expenseConverter.toResponseDTO(any(Expense.class))).willReturn(mock(ExpenseResponseDTO.class));

            // when
            expenseV2Service.updateExpense(user.getUserId(), expenseId, requestDTO, null);

            // then
            // 1. 기존 데이터가 삭제되었는지 검증
            verify(expenseSplitRepository).deleteByExpense(existingExpense);
            verify(expenseRepository).deleteByOriginalExpense(existingExpense);

            // 2. ExpenseSplitter.split이 올바른 인자로 호출되었는지 검증
            verify(expenseSplitter).split(eq(SplitType.CUSTOM), eq(requestDTO.amount()), anyList(), eq(newSplitData));

            // 3. saveAll이 올바른 데이터와 함께 호출되었는지 검증 (수정된 부분)
            ArgumentCaptor<List<ExpenseSplit>> splitListCaptor = ArgumentCaptor.forClass(List.class);
            verify(expenseSplitRepository).saveAll(splitListCaptor.capture());
            List<ExpenseSplit> savedSplits = splitListCaptor.getValue();
            assertThat(savedSplits).hasSize(2);
            assertThat(savedSplits).anyMatch(s -> s.getAmount().compareTo(new BigDecimal("6000")) == 0);

            // 4. 지출 엔티티의 다른 속성들이 잘 업데이트되었는지 검증
            assertThat(existingExpense.getTitle()).isEqualTo(requestDTO.title());
            assertThat(existingExpense.getSplitType()).isEqualTo(requestDTO.splitType());
            assertThat(existingExpense.getCategory()).isEqualTo(ExpenseCategory.OTHER);
        }
    }

}