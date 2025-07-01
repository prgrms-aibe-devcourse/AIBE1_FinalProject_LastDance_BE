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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class SSENotificationService {

    private final NotificationSettingRepository settingRepository;
    private final Map<UUID, SseEmitter> connections = new ConcurrentHashMap<>();
    private final ScheduledExecutorService heartbeatExecutor = Executors.newScheduledThreadPool(2);

    public SseEmitter createConnection(UUID userId) {
        SseEmitter emitter = new SseEmitter(0L); // 무제한 타임아웃으로 복원

        // 기존 연결 제거
        SseEmitter existing = connections.get(userId);
        if (existing != null) {
            try {
                existing.complete();
            } catch (Exception e) {
                log.warn("기존 SSE 연결 정리 중 오류: {}", e.getMessage());
            }
        }

        connections.put(userId, emitter);
        
        // 비동기로 사용자 온라인 상태 업데이트
        updateUserOnlineStatusAsync(userId, true);

        // 연결 종료 처리
        emitter.onCompletion(() -> handleDisconnection(userId));
        emitter.onTimeout(() -> {
            log.warn("SSE 연결 타임아웃: userId={}", userId);
            handleDisconnection(userId);
        });
        emitter.onError(e -> {
            log.warn("SSE 연결 오류: userId={}, error={}", userId, e.getMessage());
            handleDisconnection(userId);
        });

        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data(Map.of("status", "connected", "timestamp", LocalDateTime.now())));
            
            log.info("SSE 연결 생성: userId={}", userId);
            
            // 주기적으로 heartbeat 전송하여 연결 유지
            scheduleHeartbeat(userId, emitter);
            
        } catch (IOException e) {
            log.error("SSE 초기 메시지 전송 실패: userId={}", userId);
            handleDisconnection(userId);
        }

        return emitter;
    }

    public boolean sendNotification(UUID userId, String title, String content, NotificationType type) {
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
                    "timestamp", LocalDateTime.now()
            );

            emitter.send(SseEmitter.event()
                    .name("notification")
                    .data(data));

            log.info("SSE 알림 전송 성공: userId={}, type={}", userId, type);
            return true;

        } catch (IOException e) {
            log.warn("SSE 알림 전송 실패: userId={}, error={}", userId, e.getMessage());
            handleDisconnection(userId);
            return false;
        }
    }

    public boolean isUserOnline(UUID userId) {
        return connections.containsKey(userId);
    }

    private void handleDisconnection(UUID userId) {
        try {
            SseEmitter emitter = connections.remove(userId);
            if (emitter != null) {
                try {
                    emitter.complete();
                } catch (Exception e) {
                    log.debug("SSE emitter 정리 중 오류: {}", e.getMessage());
                }
            }
            
            // 비동기로 사용자 오프라인 상태 업데이트
            updateUserOnlineStatusAsync(userId, false);
            
        } catch (Exception e) {
            log.error("SSE 연결 해제 처리 중 오류: userId={}, error={}", userId, e.getMessage());
        }
        
        log.info("SSE 연결 해제: userId={}", userId);
    }

    // 비동기로 사용자 온라인 상태 업데이트 (커넥션 누수 방지)
    private void updateUserOnlineStatusAsync(UUID userId, boolean isOnline) {
        try {
            // 별도 스레드에서 실행하여 SSE 연결과 DB 트랜잭션 분리
            new Thread(() -> {
                try {
                    updateUserOnlineStatus(userId, isOnline);
                } catch (Exception e) {
                    log.error("사용자 온라인 상태 업데이트 실패: userId={}, isOnline={}, error={}", 
                            userId, isOnline, e.getMessage());
                }
            }).start();
        } catch (Exception e) {
            log.error("비동기 상태 업데이트 시작 실패: userId={}, error={}", userId, e.getMessage());
        }
    }

    @Transactional
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
            throw e; // 트랜잭션 롤백을 위해 예외 재발생
        }
    }

    // SSE 연결 상태 확인 및 정리 (주기적 호출용)
    public void cleanupInactiveConnections() {
        log.debug("비활성 SSE 연결 정리 시작. 현재 연결 수: {}", connections.size());
        
        connections.entrySet().removeIf(entry -> {
            UUID userId = entry.getKey();
            SseEmitter emitter = entry.getValue();
            
            try {
                // 간단한 heartbeat 전송으로 연결 상태 확인
                emitter.send(SseEmitter.event()
                        .name("heartbeat")
                        .data(Map.of("timestamp", LocalDateTime.now())));
                return false; // 연결 유지
            } catch (Exception e) {
                log.debug("비활성 SSE 연결 감지: userId={}", userId);
                updateUserOnlineStatusAsync(userId, false);
                return true; // 연결 제거
            }
        });
        
        log.debug("SSE 연결 정리 완료. 남은 연결 수: {}", connections.size());
    }

    // 현재 활성 연결 수 조회
    public int getActiveConnectionCount() {
        return connections.size();
    }

    // Heartbeat 스케줄링 - 연결 유지용
    private void scheduleHeartbeat(UUID userId, SseEmitter emitter) {
        heartbeatExecutor.scheduleWithFixedDelay(() -> {
            try {
                if (connections.containsKey(userId)) {
                    emitter.send(SseEmitter.event()
                            .name("heartbeat")
                            .data(Map.of("timestamp", LocalDateTime.now())));
                    log.debug("Heartbeat 전송: userId={}", userId);
                }
            } catch (Exception e) {
                log.debug("Heartbeat 실패로 연결 제거: userId={}, error={}", userId, e.getMessage());
                handleDisconnection(userId);
            }
        }, 30, 30, TimeUnit.SECONDS); // 30초마다 heartbeat 전송
    }

    // 서비스 종료 시 executor 정리
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
