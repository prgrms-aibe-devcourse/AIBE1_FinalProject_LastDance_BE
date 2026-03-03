package store.lastdance.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import store.lastdance.exception.CustomException;
import store.lastdance.exception.ErrorCode;
import store.lastdance.service.notification.SSENotificationV2Service;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class SSEConfig {

    private final SSENotificationV2Service sseService;

    @EventListener
    public void handleContextClosed(ContextClosedEvent event) {
        try {
            sseService.shutdown();
        } catch (CustomException e) {
            throw new CustomException(ErrorCode.NOTIFICATION_SSE_CONNECTION_CLEANUP_FAILED);
        }
    }
}
