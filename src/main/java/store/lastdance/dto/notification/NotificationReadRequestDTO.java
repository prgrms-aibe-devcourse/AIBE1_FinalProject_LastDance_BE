package store.lastdance.dto.notification;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "알림 읽음 처리 요청")
@Data
public class NotificationReadRequestDTO {
    
    @Schema(description = "알림 ID", example = "123e4567-e89b-12d3-a456-426614174000:SCHEDULE:test-12345")
    private String notificationId;
    
    @Schema(description = "읽은 시간 (선택사항)", example = "2024-01-15T10:30:00")
    private String readAt;
}
