package store.lastdance.service.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import store.lastdance.domain.notification.NotificationType;
import store.lastdance.service.onlinestatus.OnlineStatusService;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import store.lastdance.dto.notification.NotificationMessage;

@Service
@Slf4j
public class SSENotificationServiceImpl implements SSENotificationService, MessageListener { // MessageListener 구현

    private final OnlineStatusService onlineStatusService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisMessageListenerContainer redisMessageListenerContainer;
    private final ObjectMapper objectMapper;
    private final Map<UUID, SseEmitter> connections = new ConcurrentHashMap<>();
    private final ScheduledExecutorService heartbeatExecutor = Executors.newScheduledThreadPool(2, r -> {
        Thread thread = new Thread(r);
        thread.setDaemon(true); // 데몬 스레드로 설정
        return thread;
    });
    private static final String NOTIFICATION_CHANNEL = "sse-notifications";

    public SSENotificationServiceImpl(OnlineStatusService onlineStatusService, RedisTemplate<String, Object> redisTemplate, RedisMessageListenerContainer redisMessageListenerContainer, ObjectMapper objectMapper) {
        this.onlineStatusService = onlineStatusService;
        this.redisTemplate = redisTemplate;
        this.redisMessageListenerContainer = redisMessageListenerContainer;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    private void init() {
        redisMessageListenerContainer.addMessageListener(this, new ChannelTopic(NOTIFICATION_CHANNEL));
    }
    private final Map<UUID, ScheduledFuture<?>> heartbeatTasks = new ConcurrentHashMap<>();

    @Override
    public SseEmitter createConnection(UUID userId, jakarta.servlet.http.HttpServletResponse response) {
        synchronized (userId.toString().intern()) {
            disconnectUser(userId);

            SseEmitter emitter = new SseEmitter(3 * 60 * 1000L); // 3분
            connections.put(userId, emitter);

            emitter.onCompletion(() -> disconnectUser(userId));
            emitter.onTimeout(() -> {
                log.warn("SSE 연결 타임아웃: userId={}", userId);
                log.debug("SSE Timeout: Response committed status: {}", response.isCommitted());
                disconnectUser(userId);
            });
            emitter.onError(e -> {
                log.warn("SSE 연결 오류: userId={}, error={}", userId, e.getMessage());
                log.debug("SSE Error: Response committed status: {}", response.isCommitted());
                disconnectUser(userId);
            });

            try {
                if (connections.get(userId) == emitter) {
                    emitter.send(SseEmitter.event()
                            .name("connected")
                            .data(Map.of("status", "connected", "timestamp", LocalDateTime.now())));

                    log.info("SSE 연결 생성: userId={}", userId);

                    scheduleHeartbeat(userId, emitter);
                }
            } catch (IOException e) {
                log.error("SSE 초기 메시지 전송 실패: userId={}", userId);
                disconnectUser(userId);
            }

            onlineStatusService.setUserOnline(userId);
            return emitter;
        }
    }

    @Override
    public void disconnectUser(UUID userId) {
        ScheduledFuture<?> task = heartbeatTasks.remove(userId);
        if (task != null) {
            task.cancel(true);
        }
        SseEmitter emitter = connections.remove(userId);
        if (emitter != null) {
            try {
                emitter.complete();
                log.info("사용자 SSE 연결 정리: userId={}", userId);
            } catch (Exception e) {
                log.debug("SSE 연결 정리 중 오류(정상적): {}", e.getMessage());
            }
        }
        onlineStatusService.setUserOffline(userId);
    }

    @Override
    public boolean sendNotification(UUID userId, String title, String content, NotificationType type, String relatedId) {
        if (!onlineStatusService.isUserOnline(userId)) {
            log.debug("사용자 {}가 오프라인 상태이므로 SSE 알림을 발행하지 않습니다.", userId);
            return false;
        }

        try {
            NotificationMessage message = new NotificationMessage(userId, title, content, type, relatedId);
            redisTemplate.convertAndSend(NOTIFICATION_CHANNEL, message);
            log.info("Redis 채널에 알림 메시지 발행 성공: userId={}, type={}", userId, type);
            return true;
        } catch (Exception e) {
            log.error("Redis 알림 메시지 발행 실패: userId={}, error={}", userId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public void onMessage(org.springframework.data.redis.connection.Message message, byte[] pattern) {
        try {
            NotificationMessage notificationMessage = objectMapper.readValue(message.getBody(), NotificationMessage.class);
            UUID userId = notificationMessage.userId();

            SseEmitter emitter = connections.get(userId);
            if (emitter != null) {
                Map<String, Object> data = Map.of(
                        "title", notificationMessage.title(),
                        "content", notificationMessage.content(),
                        "type", notificationMessage.type().name(),
                        "icon", notificationMessage.type().getIcon(),
                        "timestamp", LocalDateTime.now(),
                        "relatedId", notificationMessage.relatedId()
                );

                emitter.send(SseEmitter.event()
                        .name("notification")
                        .data(data));

                log.info("수신된 메시지를 통해 SSE 알림 전송 성공: userId={}, type={}", userId, notificationMessage.type());
            }
        } catch (Exception e) {
            log.error("수신된 Redis 메시지 처리 중 오류 발생: {}", e.getMessage(), e);
        }
    }

    @Override
    public boolean isUserOnline(UUID userId) {
        return onlineStatusService.isUserOnline(userId);
    }

    

    // SSE 연결 상태 확인 및 정리 (주기적 호출용)
    @Override
    public void cleanupInactiveConnections() {
        log.debug("비활성 SSE 연결 정리 시작. 현재 연결 수: {}", connections.size());

        connections.entrySet().removeIf(entry -> {
            UUID userId = entry.getKey();
            SseEmitter emitter = entry.getValue();

            try {
                emitter.send(SseEmitter.event()
                        .name("heartbeat")
                        .data(Map.of("timestamp", LocalDateTime.now())));
                return false;
            } catch (Exception e) {
                log.debug("비활성 SSE 연결 감지: userId={}", userId);
                ScheduledFuture<?> task = heartbeatTasks.remove(userId);
                if (task != null) {
                    task.cancel(true);
                }
                onlineStatusService.setUserOffline(userId);
                return true;
            }
        });

        log.debug("SSE 연결 정리 완료. 남은 연결 수: {}", connections.size());
    }

    // 현재 활성 연결 수 조회
    @Override
    public int getActiveConnectionCount() {
        return connections.size();
    }

    

    // Heartbeat 스케줄링 - 연결 유지용
    private void scheduleHeartbeat(UUID userId, SseEmitter emitter) {
        // 기존 하트비트 취소
        ScheduledFuture<?> existingTask = heartbeatTasks.get(userId);
        if (existingTask != null) {
            existingTask.cancel(true);
        }

        // 새 하트비트 스케줄링
        ScheduledFuture<?> task = heartbeatExecutor.scheduleWithFixedDelay(() -> {
            try {
                // 현재 연결된 emitter와 동일한지 확인
                if (connections.get(userId) == emitter) {
                    emitter.send(SseEmitter.event()
                            .name("heartbeat")
                            .data(Map.of("timestamp", LocalDateTime.now())));
                    log.debug("Heartbeat 전송: userId={}", userId);
                }
            } catch (Exception e) {
                log.debug("Heartbeat 실패로 연결 제거: userId={}, error={}", userId, e.getMessage());
                disconnectUser(userId);
            }
        }, 30, 30, TimeUnit.SECONDS);

        heartbeatTasks.put(userId, task);
    }

    // 서비스 종료 시 executor 정리
    @Override
    public void shutdown() {
        try {
            heartbeatExecutor.shutdown();
            if (!heartbeatExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                heartbeatExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            heartbeatExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        

        // 모든 SSE 연결 정리
        connections.values().forEach(emitter -> {
            try {
                emitter.complete();
            } catch (Exception e) {
                log.debug("SSE 연결 정리 중 오류: {}", e.getMessage());
            }
        });
        connections.clear();
    }
}
