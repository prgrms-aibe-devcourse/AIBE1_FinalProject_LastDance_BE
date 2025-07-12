package store.lastdance.domain.expense;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import store.lastdance.domain.common.BaseTimeEntity;
import store.lastdance.domain.user.User;

import java.math.BigDecimal;
import java.time.LocalDate;
@Entity
@Getter
@Table(name = "expense_analysis_history")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class ExpenseAnalysisHistory extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    // 예산 사용률 정보
    @Column(nullable = false)
    private Double budgetUsagePercentage;

    @Column(nullable = false)
    private BigDecimal budgetUsageCurrentSpending;

    @Column(nullable = false)
    private BigDecimal budgetUsageTotalBudget;

    // 일평균 지출 정보
    @Column(nullable = false)
    private BigDecimal dailySpendingAverageSoFar;

    @Column(nullable = false)
    private BigDecimal dailySpendingEstimatedEom;

    // 분석 결과 요약
    @Column(nullable = false)
    private String mainFinding;

    // 개선 제안 (Suggestion)
    @Column(nullable = false)
    private String suggestionTitle;

    @Lob // 긴 텍스트를 위해
    @Column(nullable = false)
    private String suggestionDescription;

    @Column(nullable = false)
    private String suggestionEffect;

    @Column(nullable = false)
    private String suggestionDifficulty;

    @Column(name = "is_up")
    private Boolean up;

    @Column(name = "is_down")
    private Boolean down;


    @Builder
    public ExpenseAnalysisHistory(User user, LocalDate startDate, LocalDate endDate, 
                                Double budgetUsagePercentage, BigDecimal budgetUsageCurrentSpending, BigDecimal budgetUsageTotalBudget, 
                                BigDecimal dailySpendingAverageSoFar, BigDecimal dailySpendingEstimatedEom, 
                                String mainFinding, String suggestionTitle, String suggestionDescription, 
                                String suggestionEffect, String suggestionDifficulty) {
        this.user = user;
        this.startDate = startDate;
        this.endDate = endDate;
        this.budgetUsagePercentage = budgetUsagePercentage;
        this.budgetUsageCurrentSpending = budgetUsageCurrentSpending;
        this.budgetUsageTotalBudget = budgetUsageTotalBudget;
        this.dailySpendingAverageSoFar = dailySpendingAverageSoFar;
        this.dailySpendingEstimatedEom = dailySpendingEstimatedEom;
        this.mainFinding = mainFinding;
        this.suggestionTitle = suggestionTitle;
        this.suggestionDescription = suggestionDescription;
        this.suggestionEffect = suggestionEffect;
        this.suggestionDifficulty = suggestionDifficulty;
    }

    public void feedback(Boolean up, Boolean down) {
        this.up = up;
        this.down = down;
    }
}
