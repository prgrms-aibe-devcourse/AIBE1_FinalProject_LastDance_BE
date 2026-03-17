package store.lastdance.service.expense;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import store.lastdance.converter.expense.ExpenseConverter;
import store.lastdance.domain.common.ImageFile;
import store.lastdance.domain.expense.Expense;
import store.lastdance.domain.expense.ExpenseSplit;
import store.lastdance.domain.expense.ExpenseType;
import store.lastdance.domain.group.Group;
import store.lastdance.domain.group.GroupMember;
import store.lastdance.domain.user.User;
import store.lastdance.dto.expense.*;
import store.lastdance.exception.CustomException;
import store.lastdance.exception.ErrorCode;
import store.lastdance.repository.expense.ExpenseRepository;
import store.lastdance.repository.expense.ExpenseSplitRepository;
import store.lastdance.repository.group.GroupMemberRepository;
import store.lastdance.repository.group.GroupRepository;
import store.lastdance.repository.user.UserRepository;
import store.lastdance.service.image.ImageService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ExpenseV2ServiceImpl implements ExpenseV2Service {
    private final ExpenseRepository expenseRepository;
    private final ExpenseSplitRepository expenseSplitRepository;
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;
    private final ImageService imageService;
    private final ExpenseSplitter expenseSplitter;
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

    @Override
    public ExpenseResponseDTO createPersonalExpense(UUID userId, CreatePersonalExpenseRequestDTO requestDTO, MultipartFile receiptFile) {
        Expense expense = createBaseExpense(userId, requestDTO, ExpenseType.PERSONAL, receiptFile);

        try {
            Expense savedExpense = expenseRepository.save(expense);
            return expenseConverter.toResponseDTO(savedExpense);
        } catch (Exception ex) {
            ImageFile uploaded = expense.getReceiptImageFile();
            if (uploaded != null) {
                try {
                    imageService.deleteImageFromS3(uploaded.getFileId());
                } catch (Exception e) {
                    log.warn("개인지출 생성 실패시 영수증 정리 실패: {}", e.getMessage());
                }
            }
            throw ex;
        }
    }

    @Override
    public ExpenseResponseDTO createGroupExpense(UUID userId, CreateGroupExpenseRequestDTO requestDTO, MultipartFile receiptFile) {

        Group group = findGroupById(requestDTO.groupId());
        User author = findUserById(userId);
        if (!groupMemberRepository.existsByGroupAndUser(group, author)) {
            throw new CustomException(ErrorCode.GROUP_MEMBER_NOT_FOUND);
        }
        if (requestDTO.splitType() == null) {
            throw new CustomException(ErrorCode.INVALID_SPLIT_DATA);
        }

        Expense expense = createBaseExpense(userId, requestDTO, ExpenseType.GROUP, receiptFile);
        expense.updateGroup(group);
        expense.updateSplitType(requestDTO.splitType());
        try {
            Expense savedExpense = expenseRepository.save(expense);
            processGroupExpenseSplit(savedExpense, requestDTO);
            return expenseConverter.toResponseDTO(savedExpense);
        } catch (Exception ex) {
            ImageFile uploaded = expense.getReceiptImageFile();
            if (uploaded != null) {
                try {
                    imageService.deleteImageFromS3(uploaded.getFileId());
                } catch (Exception e) {
                    log.warn("그룹지출 생성 실패시 영수증 정리 실패: {}", e.getMessage());
                }
            }
            throw ex;
        }
    }

    private Expense createBaseExpense(
            UUID userId, BaseExpenseRequest request, ExpenseType expenseType, MultipartFile receiptFile
    ) {
        ImageFile uploadedImage = null;

        try {
            if (receiptFile != null && !receiptFile.isEmpty()) {
                uploadedImage = imageService.uploadImageToS3(receiptFile, "receipt-image", 10 * 1024 * 1024);
            }
            User user = findUserById(userId);

            Expense expense = Expense.builder()
                    .title(request.title())
                    .amount(request.amount())
                    .category(request.category())
                    .expenseType(expenseType)
                    .user(user)
                    .expenseDate(request.date())
                    .build();

            if (uploadedImage != null) {
                expense.updateReceiptImageFile(uploadedImage);
            }
            if (request.memo() != null) {
                expense.updateMemo(request.memo());
            }
            return expense;

        } catch (Exception e) {
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

    private void processGroupExpenseSplit(Expense original, CreateGroupExpenseRequestDTO dto) {
        List<GroupMember> groupMembers = validateAndGetGroupMembers(original);
        List<User> members = groupMembers.stream().map(GroupMember::getUser).toList();

        Map<UUID, BigDecimal> splitAmountMap = expenseSplitter.split(
                dto.splitType(),
                original.getAmount(),
                members,
                dto.splitData()
        );

        applySplitResultToDatabase(original, splitAmountMap);
    }

    private void applySplitResultToDatabase(Expense original, Map<UUID, BigDecimal> splitAmountMap) {

        List<UUID> userIds = splitAmountMap.keySet().stream().toList();
        Map<UUID, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getUserId, Function.identity()));

        var splits = new ArrayList<ExpenseSplit>(splitAmountMap.size());
        var shareExpenses = new ArrayList<Expense>(splitAmountMap.size());
        for (var entry : splitAmountMap.entrySet()) {
            var user = userMap.get(entry.getKey());
            if (user == null) {
                throw new CustomException(ErrorCode.USER_NOT_FOUND);
            }
            splits.add(ExpenseSplit.builder()
                    .expense(original)
                    .user(user)
                    .amount(entry.getValue())
                    .build());

            Expense share = Expense.builder()
                    .title(original.getTitle() + " (그룹 분담)")
                    .amount(entry.getValue())
                    .category(original.getCategory())
                    .expenseType(ExpenseType.SHARE)
                    .user(user)
                    .expenseDate(original.getExpenseDate())
                    .build();

            share.updateGroup(original.getGroup());
            share.updateMemo(original.getMemo());
            share.updateOriginalExpense(original);
            shareExpenses.add(share);
        }
        expenseSplitRepository.saveAll(splits);
        expenseRepository.saveAll(shareExpenses);
    }

    @Override
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

            if (receiptFile != null && !receiptFile.isEmpty()) {
                uploadedImage = imageService.uploadImageToS3(receiptFile, "receipt-image", 10 * 1024 * 1024);
                expense.updateReceiptImageFile(uploadedImage);

                if (oldReceiptFileId != null) {
                    imageService.deleteImageFromS3(oldReceiptFileId);
                }
            }

            if (expense.getExpenseType() == ExpenseType.GROUP) {
                if (requestDTO.splitType() == null && requestDTO.splitData() != null && !requestDTO.splitData().isEmpty()) {
                    throw new CustomException(ErrorCode.INVALID_SPLIT_DATA);
                }
                var newSplitType = requestDTO.splitType() != null ? requestDTO.splitType() : expense.getSplitType();
                if (newSplitType != null) {
                    expense.updateSplitType(newSplitType);
                    updateGroupExpenseSplits(expense, requestDTO.splitData());
                }
            }

        } catch (Exception e) {
            if (uploadedImage != null) {
                try {
                    imageService.deleteImageFromS3(uploadedImage.getFileId());
                } catch (Exception deleteEx) {
                    log.error("지출수정_고아파일 정리 실패: {}", deleteEx.getMessage());
                }
            }
            throw e;
        }

        return expenseConverter.toResponseDTO(expense);
    }

    private void updateGroupExpenseSplits(Expense original, List<SplitDataDTO> newSplitData) {
        expenseSplitRepository.deleteByExpense(original);
        expenseRepository.deleteByOriginalExpense(original);

        List<GroupMember> groupMembers = validateAndGetGroupMembers(original);
        List<User> members = groupMembers.stream().map(GroupMember::getUser).toList();

        Map<UUID, BigDecimal> splitAmountMap = expenseSplitter.split(
                original.getSplitType(),
                original.getAmount(),
                members,
                newSplitData
        );

        applySplitResultToDatabase(original, splitAmountMap);
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

    @Override
    public void deleteExpense(UUID userId, Long expenseId) {

        User user = findUserById(userId);

        Expense expense = expenseRepository.findByExpenseIdWithPermission(expenseId, user).orElseThrow(
                () -> new CustomException(ErrorCode.EXPENSE_NOT_FOUND)
        );

        if (expense.getExpenseType() == ExpenseType.GROUP) {
            expenseSplitRepository.deleteByExpense(expense);
            expenseRepository.deleteByOriginalExpense(expense);
        }

        ImageFile receipt = expense.getReceiptImageFile();
        expenseRepository.deleteById(expenseId);
        if (receipt != null) {
            try {
                imageService.deleteImageFromS3(receipt.getFileId());
            } catch (Exception e) {
                log.warn("영수증 삭제 실패: {}", e.getMessage());
            }
        }
    }

    @Override
    public void deleteReceiptImage(UUID userId, Long expenseId) {

        User user = findUserById(userId);

        Expense expense = expenseRepository.findByExpenseIdWithPermission(expenseId, user).orElseThrow(
                () -> new CustomException(ErrorCode.EXPENSE_NOT_FOUND)
        );

        ImageFile receiptImageFile = expense.getReceiptImageFile();
        if (receiptImageFile == null) {
            throw new CustomException(ErrorCode.FILE_NOT_FOUND);
        }

        imageService.deleteImageFromS3(receiptImageFile.getFileId());

        expense.updateReceiptImageFile(null);
    }
}



