package store.lastdance.dto.notification;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationSettingResponseDTO {
    private Boolean emailEnabled;
    private Boolean scheduleReminder;
    private Boolean paymentReminder;
    private Boolean checklistReminder;
}