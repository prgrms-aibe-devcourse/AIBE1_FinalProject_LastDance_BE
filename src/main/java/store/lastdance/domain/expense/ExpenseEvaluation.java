package store.lastdance.domain.expense;

import lombok.*;
import jakarta.persistence.*;
import store.lastdance.domain.common.BaseTimeEntity;

@Getter
@Entity
@Table(name = "expense_evaluations")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExpenseEvaluation extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "evaluation_id")
    private Long evaluationId;

    @Column(name = "sentiment", length = 50)
    @Enumerated(EnumType.STRING)
    private ExpenseSentiment sentiment;

    @Column(name = "feedback", columnDefinition = "TEXT")
    private String feedback;

    @Builder
    public ExpenseEvaluation(@NonNull ExpenseSentiment sentiment, String feedback) {
        this.sentiment = sentiment;
        this.feedback = feedback;
    }

    public void updateFeedback(String feedback) {
        this.feedback = feedback;
    }

    public void updateSentiment(ExpenseSentiment sentiment) {
        this.sentiment = sentiment;
    }
}
