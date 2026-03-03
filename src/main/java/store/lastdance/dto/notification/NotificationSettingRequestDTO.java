package store.lastdance.dto.notification;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import io.swagger.v3.oas.annotations.media.Schema;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "알림 설정 요청 DTO")
public class NotificationSettingRequestDTO {
    
    @Schema(description = "이메일 알림 활성화 여부", example = "true")
    private Boolean emailEnabled;
    
    @Schema(description = "일정 알림 활성화 여부", example = "true")
    private Boolean scheduleReminder;
    
    @Schema(description = "정산 알림 활성화 여부", example = "true")
    private Boolean paymentReminder;
    
    @Schema(description = "체크리스트 알림 활성화 여부", example = "true")
    private Boolean checklistReminder;

    @Schema(description = "SSE 실시간 알림 활성화 여부", example = "true")
    private Boolean sseEnabled;
}
