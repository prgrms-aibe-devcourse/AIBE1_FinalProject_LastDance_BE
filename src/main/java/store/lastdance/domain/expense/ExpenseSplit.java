package store.lastdance.domain.expense;

import jakarta.persistence.*;
import lombok.*;
import store.lastdance.domain.common.BaseTimeEntity;
import store.lastdance.domain.user.User;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Entity
@Table(name = "expense_splits")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExpenseSplit extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "split_id")
    private Long splitId;

    @Column(name = "expense_id", nullable = false)
    private Long expenseId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "paid", nullable = false)
    private Boolean paid = false;

    @Column(name = "settled_at")
    private LocalDateTime settledAt;

    // 연관관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expense_id", insertable = false, updatable = false)
    private Expense expense;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @Builder
    public ExpenseSplit(@NonNull Long expenseId, @NonNull UUID userId, @NonNull BigDecimal amount) {
        this.expenseId = expenseId;
        this.userId = userId;
        this.amount = amount;
        this.paid = false;
    }

    // 정산 완료 처리
    public void markAsPaid() {
        this.paid = true;
        this.settledAt = LocalDateTime.now();
    }

    // 정산 취소
    public void markAsUnpaid() {
        this.paid = false;
        this.settledAt = null;
    }

    // 분담 금액 수정
    public void updateAmount(BigDecimal amount) {
        this.amount = amount;
    }
}