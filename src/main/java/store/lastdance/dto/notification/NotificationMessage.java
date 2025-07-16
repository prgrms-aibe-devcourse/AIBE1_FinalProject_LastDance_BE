package store.lastdance.dto.notification;

import store.lastdance.domain.notification.NotificationType;

import java.util.UUID;

public record NotificationMessage(
    UUID userId,
    String title,
    String content,
    NotificationType type,
    String relatedId
) {}
