package store.lastdance.domain.notification;

import lombok.Getter;

@Getter
public enum NotificationType {
    SCHEDULE("일정"),
    PAYMENT("납부일"),
    CHECKLIST("할일");

    private final String description;

    NotificationType(String description) {
        this.description = description;
    }

}