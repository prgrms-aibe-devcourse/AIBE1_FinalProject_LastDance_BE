package store.lastdance.service.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
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
    private final ScheduledExecutorService heartbeatExecutor = Executors.newScheduledThreadPool(2, r -> {
        Thread thread = new Thread(r);
        thread.setDaemon(true);
        return thread;
    });
    private static final String NOTIFICATION_CHANNEL = "sse-notifications";

    public SSENotificationV2ServiceImpl(OnlineStatusService onlineStatusService, RedisTemplate<String, Object> redisTemplate, RedisMessageListenerContainer redisMessageListenerContainer, ObjectMapper objectMapper) {
        this.onlineStatusService = onlineStatusService;
        this.redisTemplate = redisTemplate;
        this.redisMessageListenerContainer = redisMessageListenerContainer;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    private void init() {
        redisMessageListenerContainer.addMessageListener(this, new ChannelTopic(NOTIFICATION_CHANNEL));
    }

    @Override
    public SseEmitter createConnection(UUID userId) {
        synchronized (userId.toString().intern()) {
            disconnectUser(userId);

            SseEmitter emitter = new SseEmitter(3 * 60 * 1000L);
            connections.put(userId, emitter);

            emitter.onCompletion(() -> disconnectUser(userId));
            emitter.onTimeout(() -> {
                disconnectUser(userId);
                throw new CustomException(ErrorCode.NOTIFICATION_SSE_CONNECTION_TIMEOUT);
            });
            emitter.onError(e -> {
                disconnectUser(userId);
                throw new CustomException(ErrorCode.NOTIFICATION_SSE_CONNECTION_FAILED);
            });

            try {
                if (connections.get(userId) == emitter) {
                    emitter.send(SseEmitter.event()
                            .name("connected")
                            .data(Map.of("status", "connected", "timestamp", LocalDateTime.now())));

                    scheduleHeartbeat(userId, emitter);
                }
            } catch (IOException e) {
                disconnectUser(userId);
                throw new CustomException(ErrorCode.NOTIFICATION_SSE_FIRST_MESSAGE_FAILED);
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
            } catch (Exception e) {
                log.debug("SSE 연결 정리 중 오류(정상적): {}", e.getMessage());
            }
        }
        onlineStatusService.setUserOffline(userId);
    }

    @Override
    public boolean sendNotification(UUID userId, String title, String content, NotificationType type, String relatedId) {
        try {
            if (!onlineStatusService.isUserOnline(userId)) {
                return false;
            }
            NotificationMessage message = new NotificationMessage(userId, title, content, type, relatedId);
            redisTemplate.convertAndSend(NOTIFICATION_CHANNEL, message);
            return true;
        } catch (CustomException e) {
            throw new CustomException(ErrorCode.NOTIFICATION_REDIS_MESSAGE_FAILED);
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
            }
        } catch (Exception e) {
            throw new CustomException(ErrorCode.NOTIFICATION_REDIS_PROCESS_FAILED);
        }
    }

    @Override
    public void cleanupInactiveConnections() {
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
            } catch (CustomException e) {
                throw new CustomException(ErrorCode.NOTIFICATION_SSE_CONNECTION_CLEANUP_FAILED);
            }
        });
        connections.clear();
    }

    private final Map<UUID, ScheduledFuture<?>> heartbeatTasks = new ConcurrentHashMap<>();

    private void scheduleHeartbeat(UUID userId, SseEmitter emitter) {
        ScheduledFuture<?> existingTask = heartbeatTasks.get(userId);
        if (existingTask != null) {
            existingTask.cancel(true);
        }

        ScheduledFuture<?> task = heartbeatExecutor.scheduleWithFixedDelay(() -> {
            try {
                if (connections.get(userId) == emitter) {
                    emitter.send(SseEmitter.event()
                            .name("heartbeat")
                            .data(Map.of("timestamp", LocalDateTime.now())));
                }
            } catch (Exception e) {
                disconnectUser(userId);
            }
        }, 30, 30, TimeUnit.SECONDS);
        heartbeatTasks.put(userId, task);
    }
}