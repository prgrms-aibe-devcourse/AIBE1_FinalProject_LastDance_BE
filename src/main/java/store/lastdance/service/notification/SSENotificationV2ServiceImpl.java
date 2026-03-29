package store.lastdance.service.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import store.lastdance.domain.notification.NotificationType;
import store.lastdance.dto.notification.NotificationMessage;
import store.lastdance.exception.CustomException;
import store.lastdance.exception.ErrorCode;
import store.lastdance.service.onlinestatus.OnlineStatusService;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

@Service
@Slf4j
public class SSENotificationV2ServiceImpl implements SSENotificationV2Service, MessageListener {

    private final OnlineStatusService onlineStatusService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisMessageListenerContainer redisMessageListenerContainer;
    private final ObjectMapper objectMapper;
    private final Map<UUID, SseEmitter> connections = new ConcurrentHashMap<>();
    private final Map<UUID, ScheduledFuture<?>> heartbeatTasks = new ConcurrentHashMap<>();
    private final Map<UUID, Object> userLocks = new ConcurrentHashMap<>();
    private final ScheduledExecutorService heartbeatExecutor;
    private static final String NOTIFICATION_CHANNEL = "sse-notifications";

    public SSENotificationV2ServiceImpl(
            OnlineStatusService onlineStatusService,
            RedisTemplate<String, Object> redisTemplate,
            RedisMessageListenerContainer redisMessageListenerContainer,
            ObjectMapper objectMapper,
            @Value("${sse.heartbeat-thread-pool-size:4}") int heartbeatThreadPoolSize) {
        this.onlineStatusService = onlineStatusService;
        this.redisTemplate = redisTemplate;
        this.redisMessageListenerContainer = redisMessageListenerContainer;
        this.objectMapper = objectMapper;
        this.heartbeatExecutor = Executors.newScheduledThreadPool(heartbeatThreadPoolSize, r -> {
            Thread thread = new Thread(r);
            thread.setName("sse-heartbeat-" + thread.getId());
            thread.setDaemon(true);
            return thread;
        });
        log.info("SSE heartbeat 스레드 풀 초기화: size={}", heartbeatThreadPoolSize);
    }

    @PostConstruct
    private void init() {
        redisMessageListenerContainer.addMessageListener(this, new ChannelTopic(NOTIFICATION_CHANNEL));
    }

    @Override
    public SseEmitter createConnection(UUID userId) {
        Object lock = userLocks.computeIfAbsent(userId, id -> new Object());
        synchronized (lock) {
            cleanupUserState(userId);

            SseEmitter emitter = new SseEmitter(3 * 60 * 1000L);
            connections.put(userId, emitter);

            emitter.onCompletion(() -> {
                Object cbLock = userLocks.get(userId);
                if (cbLock != null) {
                    synchronized (cbLock) {
                        if (connections.get(userId) == emitter) {
                            cleanupUserState(userId);
                            onlineStatusService.setUserOffline(userId);
                        }
                    }
                }
            });
            emitter.onTimeout(() -> {
                log.debug("SSE 연결 타임아웃: userId={}", userId);
                Object cbLock = userLocks.get(userId);
                if (cbLock != null) {
                    synchronized (cbLock) {
                        if (connections.get(userId) == emitter) {
                            cleanupUserState(userId);
                            onlineStatusService.setUserOffline(userId);
                        }
                    }
                }
            });
            emitter.onError(e -> {
                log.warn("SSE 연결 오류: userId={}, error={}", userId, e.getMessage());
                Object cbLock = userLocks.get(userId);
                if (cbLock != null) {
                    synchronized (cbLock) {
                        if (connections.get(userId) == emitter) {
                            cleanupUserState(userId);
                            onlineStatusService.setUserOffline(userId);
                        }
                    }
                }
            });

            try {
                emitter.send(SseEmitter.event()
                        .name("connected")
                        .data(Map.of("status", "connected", "timestamp", LocalDateTime.now())));
                scheduleHeartbeat(userId, emitter);
            } catch (IOException e) {
                cleanupUserState(userId);
                throw new CustomException(ErrorCode.NOTIFICATION_SSE_FIRST_MESSAGE_FAILED);
            }

            onlineStatusService.setUserOnline(userId);
            return emitter;
        }
    }

    @Override
    public void disconnectUser(UUID userId) {
        Object lock = userLocks.get(userId);
        if (lock == null) {
            onlineStatusService.setUserOffline(userId);
            return;
        }
        synchronized (lock) {
            cleanupUserState(userId);
            onlineStatusService.setUserOffline(userId);
        }
    }

    private void cleanupUserState(UUID userId) {
        ScheduledFuture<?> task = heartbeatTasks.remove(userId);
        if (task != null) {
            task.cancel(true);
        }
        SseEmitter emitter = connections.remove(userId);
        if (emitter != null) {
            try {
                emitter.complete();
            } catch (Exception e) {
                log.debug("SSE 연결 정리 중 오류(정상적): {}", e.getMessage());
            }
        }
    }

    @Override
    public boolean sendNotification(UUID userId, String title, String content, NotificationType type, String relatedId) {
        if (!onlineStatusService.isUserOnline(userId)) {
            return false;
        }
        try {
            NotificationMessage message = new NotificationMessage(userId, title, content, type, relatedId);
            redisTemplate.convertAndSend(NOTIFICATION_CHANNEL, message);
            return true;
        } catch (Exception e) {
            log.error("Redis 알림 메시지 발행 실패: userId={}, error={}", userId, e.getMessage());
            throw new CustomException(ErrorCode.NOTIFICATION_REDIS_MESSAGE_FAILED);
        }
    }

    @Override
    public void onMessage(org.springframework.data.redis.connection.Message message, byte[] pattern) {
        try {
            NotificationMessage notificationMessage = objectMapper.readValue(message.getBody(), NotificationMessage.class);
            UUID userId = notificationMessage.userId();

            Object lock = userLocks.get(userId);
            if (lock == null) return;

            synchronized (lock) {
                SseEmitter emitter = connections.get(userId);
                if (emitter == null) return;

                Map<String, Object> data = Map.of(
                        "title", notificationMessage.title(),
                        "content", notificationMessage.content(),
                        "type", notificationMessage.type().name(),
                        "icon", notificationMessage.type().getIcon(),
                        "timestamp", LocalDateTime.now(),
                        "relatedId", notificationMessage.relatedId()
                );
                try {
                    emitter.send(SseEmitter.event()
                            .name("notification")
                            .data(data));
                } catch (Exception sendEx) {
                    log.warn("SSE 전송 실패, 연결 정리: userId={}, error={}", userId, sendEx.getMessage());
                    cleanupUserState(userId);
                    onlineStatusService.setUserOffline(userId);
                }
            }
        } catch (Exception e) {
            log.error("Redis 메시지 처리 중 오류 발생 (알림 유실): {}", e.getMessage());
        }
    }

    @Override
    public void cleanupInactiveConnections() {
        List<UUID> deadUserIds = new ArrayList<>();

        for (Map.Entry<UUID, SseEmitter> entry : connections.entrySet()) {
            UUID userId = entry.getKey();
            SseEmitter emitter = entry.getValue();
            try {
                emitter.send(SseEmitter.event()
                        .name("ping")
                        .data(Map.of("timestamp", LocalDateTime.now())));
            } catch (Exception e) {
                log.debug("비활성 SSE 연결 감지: userId={}", userId);
                deadUserIds.add(userId);
            }
        }

        for (UUID userId : deadUserIds) {
            disconnectUser(userId);
        }
    }

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

        connections.values().forEach(emitter -> {
            try {
                emitter.complete();
            } catch (Exception e) {
                log.debug("SSE 연결 정리 중 오류: {}", e.getMessage());
            }
        });
        connections.clear();
        heartbeatTasks.clear();
        userLocks.clear();
    }

    private void scheduleHeartbeat(UUID userId, SseEmitter emitter) {
        ScheduledFuture<?> existingTask = heartbeatTasks.get(userId);
        if (existingTask != null) {
            existingTask.cancel(true);
        }

        ScheduledFuture<?> task = heartbeatExecutor.scheduleWithFixedDelay(() -> {
            Object lock = userLocks.get(userId);
            if (lock == null) return;
            synchronized (lock) {
                if (connections.get(userId) != emitter) return;
                try {
                    emitter.send(SseEmitter.event()
                            .name("heartbeat")
                            .data(Map.of("timestamp", LocalDateTime.now())));
                    onlineStatusService.refreshOnlineTTL(userId);
                } catch (Exception e) {
                    log.debug("Heartbeat 전송 실패, 연결 정리: userId={}", userId);
                    cleanupUserState(userId);
                    onlineStatusService.setUserOffline(userId);
                }
            }
        }, 30, 30, TimeUnit.SECONDS);

        heartbeatTasks.put(userId, task);
    }
}
