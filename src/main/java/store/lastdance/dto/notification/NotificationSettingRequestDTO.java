package store.lastdance.dto.notification;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NotificationSettingRequestDTO {
    private Boolean emailEnabled;
    private Boolean scheduleReminder;
    private Boolean paymentReminder;
    private Boolean checklistReminder;
}
