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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import store.lastdance.domain.common.ImageFile;
import store.lastdance.domain.expense.*;
import store.lastdance.domain.group.Group;
import store.lastdance.domain.group.GroupMember;
import store.lastdance.domain.user.OAuthProvider;
import store.lastdance.domain.user.User;
import store.lastdance.domain.user.UserRole;
import store.lastdance.dto.expense.CreateGroupExpenseRequestDTO;
import store.lastdance.dto.expense.CreatePersonalExpenseRequestDTO;
import store.lastdance.dto.expense.SplitDataDTO;
import store.lastdance.dto.expense.UpdateExpenseRequestDTO;
import store.lastdance.exception.CustomException;
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
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
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
    private MultipartFile mockMultipartFile;

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
        }
    }

    @Nested
    @DisplayName("그룹 지출 생성 테스트")
    class CreateGroupExpense {
        @Test
        @DisplayName("성공 - 균등 분할 (나머지 발생)")
        void createGroupExpense_EqualSplit_Success() {
            // given
            BigDecimal totalAmount = new BigDecimal("10000");
            CreateGroupExpenseRequestDTO requestDTO = new CreateGroupExpenseRequestDTO(
                    "Test Expense", totalAmount, ExpenseCategory.FOOD, LocalDate.now(), "Memo",
                    group.getGroupId(), SplitType.EQUAL, null
            );

            given(userRepository.findById(user.getUserId())).willReturn(Optional.of(user));
            given(groupRepository.findById(group.getGroupId())).willReturn(Optional.of(group));
            given(groupMemberRepository.findByGroup(group)).willReturn(groupMembers);
            given(expenseRepository.save(any(Expense.class))).willAnswer(invocation -> {
                Expense expense = invocation.getArgument(0);
                ReflectionTestUtils.setField(expense, "expenseId", System.nanoTime());
                return expense;
            });

            // when
            expenseV2Service.createGroupExpense(user.getUserId(), requestDTO, null);

            // then
            ArgumentCaptor<ExpenseSplit> splitCaptor = ArgumentCaptor.forClass(ExpenseSplit.class);
            verify(expenseSplitRepository, times(3)).save(splitCaptor.capture());
            List<ExpenseSplit> capturedSplits = splitCaptor.getAllValues();
            BigDecimal totalSplitAmount = capturedSplits.stream().map(ExpenseSplit::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

            long countOf3334 = capturedSplits.stream().filter(s -> s.getAmount().compareTo(new BigDecimal("3334")) == 0).count();
            long countOf3333 = capturedSplits.stream().filter(s -> s.getAmount().compareTo(new BigDecimal("3333")) == 0).count();

            assertThat(capturedSplits).hasSize(3);
            assertThat(totalSplitAmount).isEqualByComparingTo(totalAmount);
            assertThat(countOf3334).isEqualTo(1);
            assertThat(countOf3333).isEqualTo(2);

            ArgumentCaptor<Expense> expenseCaptor = ArgumentCaptor.forClass(Expense.class);
            verify(expenseRepository, times(4)).save(expenseCaptor.capture());
            long groupExpenseCount = expenseCaptor.getAllValues().stream().filter(e -> e.getExpenseType() == ExpenseType.GROUP).count();
            long shareExpenseCount = expenseCaptor.getAllValues().stream().filter(e -> e.getExpenseType() == ExpenseType.SHARE).count();
            assertThat(groupExpenseCount).isEqualTo(1);
            assertThat(shareExpenseCount).isEqualTo(3);
        }

        @Test
        @DisplayName("성공 - 지정 금액 분할")
        void createGroupExpense_CustomSplit_Success() {
            // given
            BigDecimal totalAmount = new BigDecimal("10000");
            List<SplitDataDTO> splitData = List.of(
                    new SplitDataDTO(groupUsers.get(0).getUserId(), new BigDecimal("5000")),
                    new SplitDataDTO(groupUsers.get(1).getUserId(), new BigDecimal("3000")),
                    new SplitDataDTO(groupUsers.get(2).getUserId(), new BigDecimal("2000"))
            );
            CreateGroupExpenseRequestDTO requestDTO = new CreateGroupExpenseRequestDTO(
                    "Test Expense", totalAmount, ExpenseCategory.FOOD, LocalDate.now(), "Memo",
                    group.getGroupId(), SplitType.CUSTOM, splitData
            );

            given(userRepository.findById(user.getUserId())).willReturn(Optional.of(user));
            given(groupRepository.findById(group.getGroupId())).willReturn(Optional.of(group));
            given(groupMemberRepository.findByGroup(group)).willReturn(groupMembers);
            given(userRepository.findById(groupUsers.get(0).getUserId())).willReturn(Optional.of(groupUsers.get(0)));
            given(userRepository.findById(groupUsers.get(1).getUserId())).willReturn(Optional.of(groupUsers.get(1)));
            given(userRepository.findById(groupUsers.get(2).getUserId())).willReturn(Optional.of(groupUsers.get(2)));
            given(expenseRepository.save(any(Expense.class))).willAnswer(invocation -> {
                Expense expense = invocation.getArgument(0);
                ReflectionTestUtils.setField(expense, "expenseId", System.nanoTime());
                return expense;
            });

            // when
            expenseV2Service.createGroupExpense(user.getUserId(), requestDTO, null);

            // then
            ArgumentCaptor<ExpenseSplit> splitCaptor = ArgumentCaptor.forClass(ExpenseSplit.class);
            verify(expenseSplitRepository, times(3)).save(splitCaptor.capture());
            List<ExpenseSplit> capturedSplits = splitCaptor.getAllValues();
            BigDecimal totalSplitAmount = capturedSplits.stream().map(ExpenseSplit::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

            assertThat(capturedSplits).hasSize(3);
            assertThat(totalSplitAmount).isEqualByComparingTo(totalAmount);
            assertThat(capturedSplits).anyMatch(s -> s.getUser().getUserId().equals(groupUsers.get(0).getUserId()) && s.getAmount().compareTo(new BigDecimal("5000")) == 0);
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

            List<SplitDataDTO> newSplitData = List.of(
                    new SplitDataDTO(groupUsers.get(0).getUserId(), new BigDecimal("6000")),
                    new SplitDataDTO(groupUsers.get(1).getUserId(), new BigDecimal("4000"))
            );
            UpdateExpenseRequestDTO requestDTO = new UpdateExpenseRequestDTO(
                    "Updated Title", new BigDecimal("10000"), ExpenseCategory.OTHER, LocalDate.now(), "Updated Memo",
                    newSplitData, SplitType.CUSTOM
            );

            given(userRepository.findById(user.getUserId())).willReturn(Optional.of(user));
            given(expenseRepository.findByExpenseIdWithPermission(expenseId, user)).willReturn(Optional.of(existingExpense));
            given(groupMemberRepository.findByGroup(group)).willReturn(groupMembers.subList(0, 2)); // 2명만 멤버라고 가정
            given(userRepository.findById(groupUsers.get(0).getUserId())).willReturn(Optional.of(groupUsers.get(0)));
            given(userRepository.findById(groupUsers.get(1).getUserId())).willReturn(Optional.of(groupUsers.get(1)));

            // when
            expenseV2Service.updateExpense(user.getUserId(), expenseId, requestDTO, null);

            // then
            verify(expenseSplitRepository).deleteByExpense(existingExpense);
            verify(expenseRepository).deleteByOriginalExpense(existingExpense);

            ArgumentCaptor<ExpenseSplit> splitCaptor = ArgumentCaptor.forClass(ExpenseSplit.class);
            verify(expenseSplitRepository, times(2)).save(splitCaptor.capture());
            assertThat(splitCaptor.getAllValues()).hasSize(2);
            assertThat(splitCaptor.getAllValues()).anyMatch(s -> s.getAmount().compareTo(new BigDecimal("6000")) == 0);

            assertThat(existingExpense.getTitle()).isEqualTo(requestDTO.title());
            assertThat(existingExpense.getSplitType()).isEqualTo(requestDTO.splitType());
            assertThat(existingExpense.getCategory()).isEqualTo(ExpenseCategory.OTHER);
        }
    }

    @Nested
    @DisplayName("지출 삭제 테스트")
    class DeleteExpense {
        @Test
        @DisplayName("성공 - 개인 지출 삭제")
        void deletePersonalExpense_Success() {
            // given
            Long expenseId = 1L;
            Expense personalExpense = Expense.builder()
                    .title("Personal Expense").amount(BigDecimal.TEN).category(ExpenseCategory.FOOD)
                    .expenseType(ExpenseType.PERSONAL).user(user).expenseDate(LocalDate.now()).build();
            ReflectionTestUtils.setField(personalExpense, "expenseId", expenseId);

            given(userRepository.findById(user.getUserId())).willReturn(Optional.of(user));
            given(expenseRepository.findByExpenseIdWithPermission(expenseId, user)).willReturn(Optional.of(personalExpense));

            // when
            expenseV2Service.deleteExpense(user.getUserId(), expenseId);

            // then
            verify(expenseRepository).deleteById(expenseId);
            verify(expenseSplitRepository, never()).deleteByExpense(any());
            verify(expenseRepository, never()).deleteByOriginalExpense(any());
        }

        @Test
        @DisplayName("성공 - 그룹 지출 삭제 시 관련 분담 내역 모두 삭제")
        void deleteGroupExpense_Success() {
            // given
            Long expenseId = 1L;
            Expense groupExpense = Expense.builder()
                    .title("Group Expense").amount(BigDecimal.TEN).category(ExpenseCategory.FOOD)
                    .expenseType(ExpenseType.GROUP).user(user).expenseDate(LocalDate.now()).build();
            ReflectionTestUtils.setField(groupExpense, "expenseId", expenseId);

            given(userRepository.findById(user.getUserId())).willReturn(Optional.of(user));
            given(expenseRepository.findByExpenseIdWithPermission(expenseId, user)).willReturn(Optional.of(groupExpense));

            // when
            expenseV2Service.deleteExpense(user.getUserId(), expenseId);

            // then
            verify(expenseSplitRepository).deleteByExpense(groupExpense);
            verify(expenseRepository).deleteByOriginalExpense(groupExpense);
            verify(expenseRepository).deleteById(expenseId);
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 지출")
        void deleteExpense_NotFound_Fail() {
            // given
            Long expenseId = 999L;
            given(userRepository.findById(user.getUserId())).willReturn(Optional.of(user));
            given(expenseRepository.findByExpenseIdWithPermission(expenseId, user)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> expenseV2Service.deleteExpense(user.getUserId(), expenseId))
                    .isInstanceOf(CustomException.class);
        }
    }

    @Nested
    @DisplayName("영수증 삭제 테스트")
    class DeleteReceiptImage {
        @Test
        @DisplayName("성공 - 영수증이 있는 지출")
        void deleteReceiptImage_Success() {
            // given
            Long expenseId = 1L;
            UUID fileId = UUID.randomUUID();
            ImageFile imageFile = ImageFile.builder()
                    .fileId(fileId)
                    .originalName("receipt.jpg")
                    .storedName("stored_receipt.jpg")
                    .filePath("/receipts/stored_receipt.jpg")
                    .fileSize(1024L)
                    .mimeType("image/jpeg")
                    .build();

            Expense expense = Expense.builder()
                    .title("Receipt Expense").amount(BigDecimal.TEN).category(ExpenseCategory.FOOD)
                    .expenseType(ExpenseType.PERSONAL).user(user).expenseDate(LocalDate.now()).build();
            expense.updateReceiptImageFile(imageFile);
            ReflectionTestUtils.setField(expense, "expenseId", expenseId);

            given(userRepository.findById(user.getUserId())).willReturn(Optional.of(user));
            given(expenseRepository.findByExpenseIdWithPermission(expenseId, user)).willReturn(Optional.of(expense));
            doNothing().when(imageService).deleteImageFromS3(any(UUID.class));

            // when
            expenseV2Service.deleteReceiptImage(expenseId, user.getUserId());

            // then
            verify(imageService).deleteImageFromS3(fileId);
            assertThat(expense.getReceiptImageFile()).isNull();
        }

        @Test
        @DisplayName("실패 - 영수증이 없는 지출")
        void deleteReceiptImage_NoReceipt_Fail() {
            // given
            Long expenseId = 1L;
            Expense expense = Expense.builder()
                    .title("No-Receipt Expense").amount(BigDecimal.TEN).category(ExpenseCategory.FOOD)
                    .expenseType(ExpenseType.PERSONAL).user(user).expenseDate(LocalDate.now()).build();
            ReflectionTestUtils.setField(expense, "expenseId", expenseId);

            given(userRepository.findById(user.getUserId())).willReturn(Optional.of(user));
            given(expenseRepository.findByExpenseIdWithPermission(expenseId, user)).willReturn(Optional.of(expense));

            // when & then
            assertThatThrownBy(() -> expenseV2Service.deleteReceiptImage(expenseId, user.getUserId()))
                    .isInstanceOf(CustomException.class);
        }
    }
}