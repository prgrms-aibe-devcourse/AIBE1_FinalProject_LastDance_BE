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
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.Collections;

@Service
@Slf4j
@RequiredArgsConstructor
public class SSENotificationServiceImpl implements SSENotificationService {

    private final NotificationSettingRepository settingRepository;
    private final Map<UUID, Set<SseEmitter>> connections = new ConcurrentHashMap<>();
    private final ScheduledExecutorService heartbeatExecutor = Executors.newScheduledThreadPool(2);
    private final Map<UUID, Set<ScheduledFuture<?>>> heartbeatTasks = new ConcurrentHashMap<>();
    private final ExecutorService dbUpdateExecutor = Executors.newFixedThreadPool(5);

    @Override
    public SseEmitter createConnection(UUID userId) {
        // 🔥 개선: 다중 연결 허용 (기존 연결 끊지 않음)
        SseEmitter emitter = new SseEmitter(3 * 60 * 1000L); // 3분
        
        // 연결 Set에 추가
        connections.computeIfAbsent(userId, k -> Collections.synchronizedSet(ConcurrentHashMap.newKeySet()))
                  .add(emitter);

        emitter.onCompletion(() -> removeConnection(userId, emitter));
        emitter.onTimeout(() -> {
            log.warn("SSE 연결 타임아웃: userId={}", userId);
            removeConnection(userId, emitter);
        });
        emitter.onError(e -> {
            log.warn("SSE 연결 오류: userId={}, error={}", userId, e.getMessage());
            removeConnection(userId, emitter);
        });

        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data(Map.of("status", "connected", "timestamp", LocalDateTime.now())));

            log.info("SSE 연결 생성: userId={}, 총 연결 수={}", userId, getConnectionCount(userId));

            scheduleHeartbeat(userId, emitter);
            
        } catch (IOException e) {
            log.error("SSE 초기 메시지 전송 실패: userId={}", userId);
            removeConnection(userId, emitter);
        }

        updateUserOnlineStatusAsync(userId, true);
        return emitter;
    }

    // 🔥 새로운 메서드: 특정 연결만 제거
    private void removeConnection(UUID userId, SseEmitter emitter) {
        Set<SseEmitter> userConnections = connections.get(userId);
        if (userConnections != null) {
            userConnections.remove(emitter);
            
            // 해당 emitter의 heartbeat 태스크 제거
            Set<ScheduledFuture<?>> tasks = heartbeatTasks.get(userId);
            if (tasks != null) {
                tasks.removeIf(task -> task.isDone() || task.isCancelled());
            }
            
            try {
                emitter.complete();
            } catch (Exception e) {
                log.debug("SSE 연결 정리 중 오류(정상적): {}", e.getMessage());
            }
            
            log.info("SSE 연결 제거: userId={}, 남은 연결 수={}", userId, userConnections.size());
            
            // 모든 연결이 끊어진 경우에만 오프라인 처리
            if (userConnections.isEmpty()) {
                connections.remove(userId);
                heartbeatTasks.remove(userId);
                updateUserOnlineStatusAsync(userId, false);
                log.info("모든 SSE 연결 종료: userId={}", userId);
            }
        }
    }

    @Override
    public void disconnectUser(UUID userId) {
        Set<SseEmitter> userConnections = connections.remove(userId);
        if (userConnections != null) {
            userConnections.forEach(emitter -> {
                try {
                    emitter.complete();
                } catch (Exception e) {
                    log.debug("SSE 연결 정리 중 오류(정상적): {}", e.getMessage());
                }
            });
            log.info("사용자의 모든 SSE 연결 정리: userId={}, 연결 수={}", userId, userConnections.size());
        }
        
        // 모든 heartbeat 태스크 정리
        Set<ScheduledFuture<?>> tasks = heartbeatTasks.remove(userId);
        if (tasks != null) {
            tasks.forEach(task -> {
                if (!task.isDone()) {
                    task.cancel(true);
                }
            });
        }
        
        updateUserOnlineStatusAsync(userId, false);
    }

    @Override
    public boolean sendNotification(UUID userId, String title, String content, NotificationType type, String relatedId) {
        Set<SseEmitter> userConnections = connections.get(userId);
        if (userConnections == null || userConnections.isEmpty()) {
            log.debug("SSE 연결이 없는 사용자: userId={}", userId);
            return false;
        }

        Map<String, Object> data = Map.of(
                "title", title,
                "content", content,
                "type", type.name(),
                "icon", type.getIcon(),
                "timestamp", LocalDateTime.now(),
                "relatedId", relatedId
        );

        boolean anySuccess = false;
        
        // 🔥 개선: 모든 연결에 알림 전송 (다중 탭 지원)
        for (SseEmitter emitter : userConnections.toArray(new SseEmitter[0])) {
            try {
                emitter.send(SseEmitter.event()
                        .name("notification")
                        .data(data));
                anySuccess = true;
                log.debug("SSE 알림 전송 성공 (개별 연결): userId={}", userId);
                
            } catch (IOException e) {
                log.warn("SSE 알림 전송 실패 (개별 연결): userId={}, error={}", userId, e.getMessage());
                removeConnection(userId, emitter);
            }
        }

        if (anySuccess) {
            log.info("SSE 알림 전송 성공: userId={}, type={}, 연결 수={}", userId, type, 
                    connections.getOrDefault(userId, Collections.emptySet()).size());
        }
        
        return anySuccess;
    }

    @Override
    public boolean isUserOnline(UUID userId) {
        Set<SseEmitter> userConnections = connections.get(userId);
        if (userConnections == null || userConnections.isEmpty()) {
            return false;
        }

        // 🔥 개선: ping 실패해도 바로 제거하지 않고, 하나라도 성공하면 온라인으로 판단
        boolean anySuccess = false;
        
        for (SseEmitter emitter : userConnections.toArray(new SseEmitter[0])) {
            try {
                emitter.send(SseEmitter.event()
                        .name("ping")
                        .data("ping"));
                anySuccess = true;
            } catch (Exception e) {
                log.debug("Ping 실패한 연결 제거: userId={}, error={}", userId, e.getMessage());
                removeConnection(userId, emitter);
            }
        }
        
        return anySuccess;
    }

    // 비동기로 사용자 온라인 상태 업데이트
    private void updateUserOnlineStatusAsync(UUID userId, boolean isOnline) {
        dbUpdateExecutor.submit(() -> {
            try {
                updateUserOnlineStatus(userId, isOnline);
            } catch (Exception e) {
                log.error("사용자 온라인 상태 업데이트 실패: userId={}, isOnline={}, error={}",
                        userId, isOnline, e.getMessage());
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
                        log.debug("사용자 온라인 상태 업데이트: userId={}, isOnline={}", userId, isOnline);
                    },
                    () -> {
                        // 설정이 없으면 새로 생성
                        if (isOnline) {
                            NotificationSetting newSetting = NotificationSetting.builder()
                                    .userId(userId)
                                    .build();
                            newSetting.updateOnlineStatus(true);
                            settingRepository.save(newSetting);
                            log.info("새 알림 설정 생성 및 온라인 상태 설정: userId={}", userId);
                        }
                    }
            );
        } catch (Exception e) {
            log.error("사용자 온라인 상태 업데이트 중 데이터베이스 오류: userId={}, isOnline={}, error={}",
                    userId, isOnline, e.getMessage(), e);
            // 예외를 던지지 않음 (SSE에 영향 차단)
        }
    }

    // SSE 연결 상태 확인 및 정리 (주기적 호출용) - 개선된 버전
    @Override
    public void cleanupInactiveConnections() {
        int totalConnections = getActiveConnectionCount();
        log.debug("비활성 SSE 연결 정리 시작. 현재 연결 수: {}", totalConnections);

        connections.entrySet().removeIf(entry -> {
            UUID userId = entry.getKey();
            Set<SseEmitter> userConnections = entry.getValue();
            
            userConnections.removeIf(emitter -> {
                try {
                    emitter.send(SseEmitter.event()
                            .name("heartbeat")
                            .data(Map.of("timestamp", LocalDateTime.now())));
                    return false; // 유지
                } catch (Exception e) {
                    log.debug("비활성 SSE 연결 감지: userId={}", userId);
                    return true; // 제거
                }
            });
            
            // 모든 연결이 제거된 사용자는 Map에서도 제거
            if (userConnections.isEmpty()) {
                Set<ScheduledFuture<?>> tasks = heartbeatTasks.remove(userId);
                if (tasks != null) {
                    tasks.forEach(task -> {
                        if (!task.isDone()) {
                            task.cancel(true);
                        }
                    });
                }
                updateUserOnlineStatusAsync(userId, false);
                return true;
            }
            
            return false;
        });

        int remainingConnections = getActiveConnectionCount();
        log.debug("SSE 연결 정리 완료. 남은 연결 수: {}", remainingConnections);
    }

    // 현재 활성 연결 수 조회 (전체)
    @Override
    public int getActiveConnectionCount() {
        return connections.values().stream()
                .mapToInt(Set::size)
                .sum();
    }
    
    // 🔥 새로운 메서드: 특정 사용자의 연결 수 조회
    public int getConnectionCount(UUID userId) {
        Set<SseEmitter> userConnections = connections.get(userId);
        return userConnections != null ? userConnections.size() : 0;
    }

    // Heartbeat 스케줄링 - 연결 유지용 (개선된 버전)
    private void scheduleHeartbeat(UUID userId, SseEmitter emitter) {
        ScheduledFuture<?> task = heartbeatExecutor.scheduleWithFixedDelay(() -> {
            try {
                // 연결이 여전히 유효한지 확인
                Set<SseEmitter> userConnections = connections.get(userId);
                if (userConnections != null && userConnections.contains(emitter)) {
                    emitter.send(SseEmitter.event()
                            .name("heartbeat")
                            .data(Map.of("timestamp", LocalDateTime.now())));
                    log.debug("Heartbeat 전송: userId={}", userId);
                }
            } catch (Exception e) {
                log.debug("Heartbeat 실패로 연결 제거: userId={}, error={}", userId, e.getMessage());
                removeConnection(userId, emitter);
            }
        }, 30, 30, TimeUnit.SECONDS);

        // heartbeat 태스크 저장
        heartbeatTasks.computeIfAbsent(userId, k -> Collections.synchronizedSet(ConcurrentHashMap.newKeySet()))
                     .add(task);
    }

    // 서비스 종료 시 executor 정리 - 개선된 버전
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

        // 모든 SSE 연결 정리 (개선된 버전)
        connections.values().forEach(userConnections -> {
            userConnections.forEach(emitter -> {
                try {
                    emitter.complete();
                } catch (Exception e) {
                    log.debug("SSE 연결 정리 중 오류: {}", e.getMessage());
                }
            });
        });
        connections.clear();
        
        // 모든 heartbeat 태스크 정리
        heartbeatTasks.values().forEach(tasks -> {
            tasks.forEach(task -> {
                if (!task.isDone()) {
                    task.cancel(true);
                }
            });
        });
        heartbeatTasks.clear();
    }
}
