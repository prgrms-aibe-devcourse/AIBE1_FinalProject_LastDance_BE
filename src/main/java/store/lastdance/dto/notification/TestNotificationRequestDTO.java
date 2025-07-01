package store.lastdance.dto.notification;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "테스트 알림 전송 요청")
@Data
public class TestNotificationRequestDTO {
    
    @Schema(description = "알림 타입", example = "SCHEDULE", allowableValues = {"SCHEDULE", "PAYMENT", "CHECKLIST"})
    private String type;
    
    @Schema(description = "알림 제목", example = "테스트 알림")
    private String title;
    
    @Schema(description = "알림 내용", example = "하이브리드 알림 시스템이 정상적으로 작동합니다! 🎉")
    private String content;
    
    @Schema(description = "관련 ID", example = "test-12345")
    private String relatedId;
}
