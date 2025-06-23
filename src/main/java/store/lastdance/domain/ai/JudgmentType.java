package store.lastdance.domain.ai;

public enum JudgmentType {
    GROUP("Group"),
    PERSONAL("Personal");

    private final String description;

    JudgmentType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}