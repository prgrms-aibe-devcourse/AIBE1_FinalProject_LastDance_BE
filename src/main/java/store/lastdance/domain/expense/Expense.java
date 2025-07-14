package store.lastdance.domain.expense;

import jakarta.persistence.*;
import lombok.*;
import store.lastdance.domain.common.BaseTimeEntity;
import store.lastdance.domain.common.ImageFile;
import store.lastdance.domain.group.Group;
import store.lastdance.domain.user.User;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Entity
@Table(name = "expenses")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Expense extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "expense_id")
    private Long expenseId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "category", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private ExpenseCategory category;

    @Column(name = "expense_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ExpenseType expenseType;

    @Setter
    @Column(name = "split_type", length = 20)
    @Enumerated(EnumType.STRING)
    private SplitType splitType;

    @Column(name = "expense_date", nullable = false)
    private LocalDate expenseDate;

    @Column(name = "memo", columnDefinition = "TEXT")
    private String memo;

    @Column(name = "is_settled", nullable = false)
    private Boolean isSettled = false;

    // 연관관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // === 설정 메서드들 ===
    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private Group group;

    @Setter
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receipt_image_file_id")
    private ImageFile receiptImageFile;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_expense_id")
    private Expense originalExpense;

    @Builder
    public Expense(@NonNull String title, @NonNull BigDecimal amount, @NonNull ExpenseCategory category,
                   @NonNull ExpenseType expenseType, @NonNull User user, @NonNull LocalDate expenseDate) {
        this.title = title;
        this.amount = amount;
        this.category = category;
        this.expenseType = expenseType;
        this.user = user;
        this.expenseDate = expenseDate;
        this.isSettled = false;
    }

    // === 업데이트 메서드들 ===
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

    public void updateExpenseDate(LocalDate expenseDate) {
        this.expenseDate = expenseDate;
    }

}