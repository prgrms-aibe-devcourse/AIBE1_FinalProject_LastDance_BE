package store.lastdance.domain.expense;

import lombok.*;
import jakarta.persistence.*;
import store.lastdance.domain.group.Group;
import store.lastdance.domain.user.ImageFile;
import store.lastdance.domain.user.User;
import store.lastdance.domain.common.BaseTimeEntity;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Entity
@Table(name = "expenses")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Expense extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "expense_id")
    private Long expenseId;

    @Column(name = "original_expense_id")
    private Long originalExpenseId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "category", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private ExpenseCategory category;

    @Column(name = "type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ExpenseType type;

    @Column(name = "group_id")
    private UUID groupId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "expense_date", nullable = false)
    private LocalDate expenseDate;

    @Column(name = "receipt_image_file_id")
    private UUID receiptImageFileId;

    @Column(name = "expense_type", length = 50)
    private String expenseType;

    @Column(name = "memo", columnDefinition = "TEXT")
    private String memo;

    @Column(name = "is_settlement", nullable = false)
    private Boolean isSettlement = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", insertable = false, updatable = false)
    private Group group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receipt_image_file_id", insertable = false, updatable = false)
    private ImageFile receiptImageFile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_expense_id", insertable = false, updatable = false)
    private Expense originalExpense;

    @Builder
    public Expense(@NonNull String title, @NonNull BigDecimal amount, @NonNull ExpenseCategory category,
                   @NonNull ExpenseType type, @NonNull UUID userId, @NonNull LocalDate expenseDate) {
        this.title = title;
        this.amount = amount;
        this.category = category;
        this.type = type;
        this.userId = userId;
        this.expenseDate = expenseDate;
        this.isSettlement = false;
    }

    public void updateTitle(String title) {
        this.title = title;
    }

    public void updateAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public void updateCategory(ExpenseCategory category) {
        this.category = category;
    }

    public void updateMemo(String memo) {
        this.memo = memo;
    }

    public void addReceiptImage(UUID receiptImageFileId) {
        this.receiptImageFileId = receiptImageFileId;
    }

    public void markAsSettlement() {
        this.isSettlement = true;
    }

    public void setGroupId(UUID groupId) {
        this.groupId = groupId;
    }

    public void setExpenseType(String expenseType) {
        this.expenseType = expenseType;
    }
}
