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

    private final Map<UUID, ConcurrentHashMap<String, SseEmitter>> connections = new ConcurrentHashMap<>();
    private final Map<UUID, ConcurrentHashMap<String, ScheduledFuture<?>>> heartbeatTasks = new ConcurrentHashMap<>();
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
    public SseEmitter createConnection(UUID userId, String connectionId) {
        Object lock = userLocks.computeIfAbsent(userId, id -> new Object());
        synchronized (lock) {
            connections.computeIfAbsent(userId, id -> new ConcurrentHashMap<>());
            heartbeatTasks.computeIfAbsent(userId, id -> new ConcurrentHashMap<>());

            SseEmitter emitter = new SseEmitter(3 * 60 * 1000L);
            connections.get(userId).put(connectionId, emitter);

            emitter.onCompletion(() -> cleanupConnection(userId, connectionId, emitter));
            emitter.onTimeout(() -> {
                log.debug("SSE 연결 타임아웃: userId={}, connectionId={}", userId, connectionId);
                cleanupConnection(userId, connectionId, emitter);
            });
            emitter.onError(e -> {
                log.warn("SSE 연결 오류: userId={}, connectionId={}, error={}", userId, connectionId, e.getMessage());
                cleanupConnection(userId, connectionId, emitter);
            });

            try {
                emitter.send(SseEmitter.event()
                        .name("connected")
                        .data(Map.of("status", "connected", "connectionId", connectionId, "timestamp", LocalDateTime.now())));
                scheduleHeartbeat(userId, connectionId, emitter);
            } catch (IOException e) {
                cleanupSingleConnection(userId, connectionId);
                throw new CustomException(ErrorCode.NOTIFICATION_SSE_FIRST_MESSAGE_FAILED);
            }

            if (connections.get(userId).size() == 1) {
                onlineStatusService.setUserOnline(userId);
            }

            log.debug("SSE 연결 추가: userId={}, connectionId={}, 총 연결 수={}",
                    userId, connectionId, connections.get(userId).size());
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
            cleanupAllConnections(userId);
            onlineStatusService.setUserOffline(userId);
        }
    }

    @Override
    public void disconnectConnection(UUID userId, String connectionId) {
        Object lock = userLocks.get(userId);
        if (lock == null) return;
        synchronized (lock) {
            cleanupSingleConnection(userId, connectionId);
            Map<String, SseEmitter> userConnections = connections.get(userId);
            if (userConnections == null || userConnections.isEmpty()) {
                onlineStatusService.setUserOffline(userId);
            }
        }
    }

    private void cleanupConnection(UUID userId, String connectionId, SseEmitter emitter) {
        Object lock = userLocks.get(userId);
        if (lock == null) return;
        synchronized (lock) {
            Map<String, SseEmitter> userConnections = connections.get(userId);
            if (userConnections == null) return;
            if (userConnections.get(connectionId) != emitter) return;

            cleanupSingleConnection(userId, connectionId);

            if (userConnections.isEmpty()) {
                onlineStatusService.setUserOffline(userId);
                log.debug("마지막 SSE 연결 해제 → 오프라인: userId={}", userId);
            }
        }
    }

    private void cleanupSingleConnection(UUID userId, String connectionId) {
        ConcurrentHashMap<String, ScheduledFuture<?>> userTasks = heartbeatTasks.get(userId);
        if (userTasks != null) {
            ScheduledFuture<?> task = userTasks.remove(connectionId);
            if (task != null) task.cancel(true);
        }

        ConcurrentHashMap<String, SseEmitter> userConnections = connections.get(userId);
        if (userConnections != null) {
            SseEmitter emitter = userConnections.remove(connectionId);
            if (emitter != null) {
                try {
                    emitter.complete();
                } catch (Exception e) {
                    log.debug("SSE 연결 정리 중 오류(정상적): {}", e.getMessage());
                }
            }
        }

        if (userConnections != null && userConnections.isEmpty()) {
            connections.remove(userId);
            heartbeatTasks.remove(userId);
            userLocks.remove(userId);
            log.debug("SSE 유저 상태 완전 제거 (마지막 연결 정리): userId={}", userId);
        }

        log.debug("SSE 단일 연결 정리: userId={}, connectionId={}", userId, connectionId);
    }

    private void cleanupAllConnections(UUID userId) {
        ConcurrentHashMap<String, ScheduledFuture<?>> userTasks = heartbeatTasks.remove(userId);
        if (userTasks != null) {
            userTasks.values().forEach(task -> task.cancel(true));
        }

        ConcurrentHashMap<String, SseEmitter> userConnections = connections.remove(userId);
        if (userConnections != null) {
            userConnections.values().forEach(emitter -> {
                try {
                    emitter.complete();
                } catch (Exception e) {
                    log.debug("SSE 전체 연결 정리 중 오류(정상적): {}", e.getMessage());
                }
            });
        }

        userLocks.remove(userId);
        log.debug("SSE 전체 연결 정리 완료: userId={}", userId);
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
                ConcurrentHashMap<String, SseEmitter> userConnections = connections.get(userId);
                if (userConnections == null || userConnections.isEmpty()) return;

                Map<String, Object> data = Map.of(
                        "title", notificationMessage.title(),
                        "content", notificationMessage.content(),
                        "type", notificationMessage.type().name(),
                        "icon", notificationMessage.type().getIcon(),
                        "timestamp", LocalDateTime.now(),
                        "relatedId", notificationMessage.relatedId()
                );

                List<String> deadConnectionIds = new ArrayList<>();
                for (Map.Entry<String, SseEmitter> entry : userConnections.entrySet()) {
                    try {
                        entry.getValue().send(SseEmitter.event()
                                .name("notification")
                                .data(data));
                    } catch (Exception sendEx) {
                        log.warn("SSE 전송 실패: userId={}, connectionId={}, error={}",
                                userId, entry.getKey(), sendEx.getMessage());
                        deadConnectionIds.add(entry.getKey());
                    }
                }

                for (String deadId : deadConnectionIds) {
                    cleanupSingleConnection(userId, deadId);
                }
                if (userConnections.isEmpty()) {
                    onlineStatusService.setUserOffline(userId);
                }
            }
        } catch (Exception e) {
            log.error("Redis 메시지 처리 중 오류 발생 (알림 유실): {}", e.getMessage());
        }
    }

    @Override
    public void cleanupInactiveConnections() {
        for (UUID userId : new ArrayList<>(connections.keySet())) {
            Object lock = userLocks.get(userId);
            if (lock == null) continue;
            synchronized (lock) {
                ConcurrentHashMap<String, SseEmitter> userConnections = connections.get(userId);
                if (userConnections == null) continue;

                List<String> deadConnectionIds = new ArrayList<>();
                for (Map.Entry<String, SseEmitter> entry : userConnections.entrySet()) {
                    try {
                        entry.getValue().send(SseEmitter.event()
                                .name("ping")
                                .data(Map.of("timestamp", LocalDateTime.now())));
                    } catch (Exception e) {
                        deadConnectionIds.add(entry.getKey());
                    }
                }

                for (String deadId : deadConnectionIds) {
                    cleanupSingleConnection(userId, deadId);
                }
                if (userConnections.isEmpty()) {
                    onlineStatusService.setUserOffline(userId);
                }
            }
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

        connections.values().forEach(userConnections ->
                userConnections.values().forEach(emitter -> {
                    try {
                        emitter.complete();
                    } catch (Exception e) {
                        log.debug("SSE shutdown 정리 중 오류: {}", e.getMessage());
                    }
                })
        );
        connections.clear();
        heartbeatTasks.clear();
        userLocks.clear();
    }

    private void scheduleHeartbeat(UUID userId, String connectionId, SseEmitter emitter) {
        ScheduledFuture<?> task = heartbeatExecutor.scheduleWithFixedDelay(() -> {
            Object lock = userLocks.get(userId);
            if (lock == null) return;
            synchronized (lock) {
                ConcurrentHashMap<String, SseEmitter> userConnections = connections.get(userId);
                if (userConnections == null) return;
                if (userConnections.get(connectionId) != emitter) return;
                try {
                    emitter.send(SseEmitter.event()
                            .name("heartbeat")
                            .data(Map.of("timestamp", LocalDateTime.now())));
                    onlineStatusService.refreshOnlineTTL(userId);
                } catch (Exception e) {
                    log.debug("Heartbeat 전송 실패, 연결 정리: userId={}, connectionId={}", userId, connectionId);
                    cleanupSingleConnection(userId, connectionId);
                    ConcurrentHashMap<String, SseEmitter> remaining = connections.get(userId);
                    if (remaining == null || remaining.isEmpty()) {
                        onlineStatusService.setUserOffline(userId);
                    }
                }
            }
        }, 30, 30, TimeUnit.SECONDS);

        heartbeatTasks.get(userId).put(connectionId, task);
    }
}
