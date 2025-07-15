package store.lastdance.service.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import store.lastdance.domain.notification.NotificationSetting;
import store.lastdance.domain.notification.NotificationType;
import store.lastdance.repository.notification.NotificationSettingRepository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

@Service
@RequiredArgsConstructor
public class SSENotificationServiceImpl implements SSENotificationService {

    private final NotificationSettingRepository settingRepository;
    private final Map<UUID, SseEmitter> connections = new ConcurrentHashMap<>();
    private final ScheduledExecutorService heartbeatExecutor = Executors.newScheduledThreadPool(2);
    private final Map<UUID, ScheduledFuture<?>> heartbeatTasks = new ConcurrentHashMap<>();
    private final ExecutorService dbUpdateExecutor = Executors.newFixedThreadPool(5);

    @Override
    public SseEmitter createConnection(UUID userId) {
        synchronized (userId.toString().intern()) {
            disconnectUser(userId);

            SseEmitter emitter = new SseEmitter(3 * 60 * 1000L); // 3분
            connections.put(userId, emitter);

            emitter.onCompletion(() -> disconnectUser(userId));
            emitter.onTimeout(() -> {
                disconnectUser(userId);
            });
            emitter.onError(e -> {
                disconnectUser(userId);
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
            }

            updateUserOnlineStatusAsync(userId, true);
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
            }
        }
        updateUserOnlineStatusAsync(userId, false);
    }

    @Override
    public boolean sendNotification(UUID userId, String title, String content, NotificationType type, String relatedId) {
        SseEmitter emitter = connections.get(userId);
        if (emitter == null) {
            return false;
        }

        try {
            Map<String, Object> data = Map.of(
                    "title", title,
                    "content", content,
                    "type", type.name(),
                    "icon", type.getIcon(),
                    "timestamp", LocalDateTime.now(),
                    "relatedId", relatedId
            );

            emitter.send(SseEmitter.event()
                    .name("notification")
                    .data(data));

            return true;

        } catch (IOException e) {
            disconnectUser(userId);
            return false;
        }
    }

    @Override
    public boolean isUserOnline(UUID userId) {
        SseEmitter emitter = connections.get(userId);
        if (emitter == null) {
            return false;
        }

        try {
            emitter.send(SseEmitter.event()
                    .name("ping")
                    .data("ping"));
            return true;
        } catch (Exception e) {
            // 연결 실패 시 즉시 정리
            disconnectUser(userId);
            return false;
        }
    }

    // 비동기로 사용자 온라인 상태 업데이트
    private void updateUserOnlineStatusAsync(UUID userId, boolean isOnline) {
        dbUpdateExecutor.submit(() -> {
            try {
                updateUserOnlineStatus(userId, isOnline);
            } catch (Exception e) {
            }
        });
    }

    @Override
    public void updateUserOnlineStatus(UUID userId, boolean isOnline) {
        try {
            settingRepository.findByUserId(userId).ifPresentOrElse(
                    setting -> {
                        setting.updateOnlineStatus(isOnline);
                        settingRepository.save(setting);
                    },
                    () -> {
                        // 설정이 없으면 새로 생성
                        if (isOnline) {
                            NotificationSetting newSetting = NotificationSetting.builder()
                                    .userId(userId)
                                    .build();
                            newSetting.updateOnlineStatus(true);
                            settingRepository.save(newSetting);
                        }
                    }
            );
        } catch (Exception e) {
            // 예외를 던지지 않음 (SSE에 영향 차단)
        }
    }

    // SSE 연결 상태 확인 및 정리 (주기적 호출용)
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
                ScheduledFuture<?> task = heartbeatTasks.remove(userId);
                if (task != null) {
                    task.cancel(true);
                }
                updateUserOnlineStatusAsync(userId, false);
                return true;
            }
        });

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
                }
            } catch (Exception e) {
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

        // DB 업데이트 executor 정리
        dbUpdateExecutor.shutdown();
        try {
            if (!dbUpdateExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                dbUpdateExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            dbUpdateExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        // 모든 SSE 연결 정리
        connections.values().forEach(emitter -> {
            try {
                emitter.complete();
            } catch (Exception e) {
            }
        });
        connections.clear();
    }
}
