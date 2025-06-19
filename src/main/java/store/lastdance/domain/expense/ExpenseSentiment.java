package store.lastdance.domain.expense;

public enum ExpenseSentiment {
    HAMBURGER_LOVER("햄버거 러버"),
    CAFFEINE_ADDICT("카페인 중독자");

    private final String description;

    ExpenseSentiment(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
