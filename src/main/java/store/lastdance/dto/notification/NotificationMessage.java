package store.lastdance.dto.notification;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import store.lastdance.domain.notification.NotificationType;

import java.util.UUID;

public record NotificationMessage(
        @JsonProperty("userId") UUID userId,
        @JsonProperty("title") String title,
        @JsonProperty("content") String content,
        @JsonProperty("type") NotificationType type,
        @JsonProperty("relatedId") String relatedId
) {
    @JsonCreator
    public NotificationMessage {
    }
}
