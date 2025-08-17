package store.lastdance.domain.expense;

public enum SplitType {
    EQUAL("균등분할"),
    CUSTOM("비율지정"),
    SPECIFIC("금액지정");

    private final String description;

    SplitType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
