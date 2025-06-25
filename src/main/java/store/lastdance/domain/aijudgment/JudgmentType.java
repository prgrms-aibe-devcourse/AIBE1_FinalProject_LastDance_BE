package store.lastdance.domain.aijudgment;

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