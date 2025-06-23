package store.lastdance.domain.calendar;

public enum ScheduleCategory {
    PAYMENT("납부"),
    CLEANING("청소"),
    OTHER("기타");

    private final String description;

    ScheduleCategory(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}