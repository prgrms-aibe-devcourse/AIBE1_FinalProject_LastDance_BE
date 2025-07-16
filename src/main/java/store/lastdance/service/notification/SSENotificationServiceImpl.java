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
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;

@Service
@Slf4j
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
                log.warn("SSE 연결 타임아웃: userId={}", userId);
                disconnectUser(userId);
            });
            emitter.onError(e -> {
                log.warn("SSE 연결 오류: userId={}, error={}", userId, e.getMessage());
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

            updateUserOnlineStatusSync(userId, true);
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
        updateUserOnlineStatusSync(userId, false);
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

            log.info("SSE 알림 전송 성공: userId={}, type={}", userId, type);
            return true;

        } catch (IOException e) {
            log.warn("SSE 알림 전송 실패: userId={}, error={}", userId, e.getMessage());
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

    // 사용자 온라인 상태 업데이트 (동기식으로 변경)
    private void updateUserOnlineStatusSync(UUID userId, boolean isOnline) {
        try {
            updateUserOnlineStatus(userId, isOnline);
        } catch (Exception e) {
            log.error("사용자 온라인 상태 업데이트 실패: userId={}, isOnline={}, error={}",
                    userId, isOnline, e.getMessage(), e);
            // SSE 연결에는 영향을 주지 않도록 예외를 삼킴
        }
    }

    @Override
    @Transactional
    public void updateUserOnlineStatus(UUID userId, boolean isOnline) {
        try {
            log.debug("사용자 온라인 상태 업데이트 시도: userId={}, isOnline={}", userId, isOnline);
            
            Optional<NotificationSetting> settingOpt = settingRepository.findByUserId(userId);
            
            if (settingOpt.isPresent()) {
                NotificationSetting setting = settingOpt.get();
                setting.updateOnlineStatus(isOnline);
                NotificationSetting savedSetting = settingRepository.save(setting);
                log.info("사용자 온라인 상태 업데이트 성공: userId={}, isOnline={}, lastSeen={}", 
                        userId, isOnline, savedSetting.getLastSeen());
            } else {
                // 설정이 없으면 새로 생성
                if (isOnline) {
                    NotificationSetting newSetting = NotificationSetting.builder()
                            .userId(userId)
                            .build();
                    newSetting.updateOnlineStatus(true);
                    NotificationSetting savedSetting = settingRepository.save(newSetting);
                    log.info("새 알림 설정 생성 및 온라인 상태 설정 성공: userId={}, lastSeen={}", 
                            userId, savedSetting.getLastSeen());
                } else {
                    log.debug("오프라인 상태로 변경 요청이지만 설정이 존재하지 않아 무시: userId={}", userId);
                }
            }
        } catch (Exception e) {
            log.error("사용자 온라인 상태 업데이트 중 데이터베이스 오류: userId={}, isOnline={}, error={}",
                    userId, isOnline, e.getMessage(), e);
            throw e; // 예외를 다시 던져서 호출자가 알 수 있도록 함
        }
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
                updateUserOnlineStatusSync(userId, false);
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

    // 특정 사용자의 실제 온라인 상태 확인 (DB와 메모리 상태 비교)
    public boolean isUserActuallyOnline(UUID userId) {
        // 메모리에 연결이 있는지 확인
        boolean hasConnection = connections.containsKey(userId);
        
        if (!hasConnection) {
            // 연결이 없으면 DB 상태도 오프라인으로 업데이트
            updateUserOnlineStatusSync(userId, false);
            return false;
        }
        
        // 연결이 있으면 실제로 유효한지 ping 테스트
        try {
            SseEmitter emitter = connections.get(userId);
            if (emitter != null) {
                emitter.send(SseEmitter.event()
                        .name("ping")
                        .data("status_check"));
                
                // 연결이 유효하면 DB 상태도 온라인으로 업데이트
                updateUserOnlineStatusSync(userId, true);
                return true;
            }
        } catch (Exception e) {
            log.debug("사용자 온라인 상태 확인 중 연결 실패: userId={}", userId);
            disconnectUser(userId);
        }
        
        return false;
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
                log.debug("SSE 연결 정리 중 오류: {}", e.getMessage());
            }
        });
        connections.clear();
    }
}
