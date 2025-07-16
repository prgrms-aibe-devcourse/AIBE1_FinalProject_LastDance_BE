package store.lastdance.dto.notification;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationSettingResponseDTO {
    private Long settingId;
    private UUID userId;
    private Boolean emailEnabled;
    private Boolean scheduleReminder;
    private Boolean paymentReminder;
    private Boolean checklistReminder;
    private Boolean sseEnabled;
    private LocalDateTime createdAt;
}