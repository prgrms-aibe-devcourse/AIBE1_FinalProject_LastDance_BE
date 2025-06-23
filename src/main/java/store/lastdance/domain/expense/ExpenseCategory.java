package store.lastdance.domain.expense;

public enum ExpenseCategory {
    FOOD("식비"),
    UTILITIES("공과금"),
    SHOPPING("쇼핑"),
    ENTERTAINMENT("유흥"),
    OTHER("기타");

    private final String description;

    ExpenseCategory(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}