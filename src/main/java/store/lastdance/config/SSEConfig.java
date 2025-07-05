package store.lastdance.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import store.lastdance.service.notification.SSENotificationServiceImpl;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class SSEConfig {

    private final SSENotificationServiceImpl sseService;

    @EventListener
    public void handleContextClosed(ContextClosedEvent event) {
        log.info("애플리케이션 종료 중 - SSE 연결 정리 시작");
        try {
            sseService.shutdown();
            log.info("SSE 연결 정리 완료");
        } catch (Exception e) {
            log.error("SSE 연결 정리 중 오류 발생: {}", e.getMessage(), e);
        }
    }
}
