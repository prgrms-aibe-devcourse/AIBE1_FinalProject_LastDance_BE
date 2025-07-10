package store.lastdance.domain.expense;

public enum ExpenseType {
    PERSONAL("개인지출"),
    GROUP("그룹지출"),
    SHARE("그룹 분담지출");

    private final String description;

    ExpenseType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}