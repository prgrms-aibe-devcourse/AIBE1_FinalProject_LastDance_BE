package store.lastdance.dto.notification;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "웹푸시 구독 요청")
@Data
public class WebPushSubscriptionRequest {
    
    @Schema(description = "푸시 서비스 엔드포인트", example = "https://fcm.googleapis.com/fcm/send/...")
    private String endpoint;
    
    @Schema(description = "암호화 키")
    private Keys keys;

    @Schema(description = "웹푸시 암호화 키")
    @Data
    public static class Keys {
        @Schema(description = "P256DH 키", example = "BEl62iUYgUivxIkv69yViA...")
        private String p256dh;
        
        @Schema(description = "Auth 키", example = "tBHItJI5svbpez7KI4CCXg")
        private String auth;
    }
}