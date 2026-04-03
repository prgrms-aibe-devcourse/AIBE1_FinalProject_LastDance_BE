package store.lastdance.service.notification.sse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import store.lastdance.domain.notification.NotificationType;
import store.lastdance.dto.notification.NotificationMessage;
import store.lastdance.exception.CustomException;
import store.lastdance.exception.ErrorCode;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SSENotificationV2ServiceImpl 테스트")
class SSENotificationV2ServiceImplTest {

    @Mock
    private OnlineStatusService onlineStatusService;
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private RedisMessageListenerContainer redisMessageListenerContainer;

    private SSENotificationV2ServiceImpl service;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @BeforeEach
    void setUp() {
        service = new SSENotificationV2ServiceImpl(
                onlineStatusService, redisTemplate, redisMessageListenerContainer, objectMapper, 2);
    }

    // ──────────────────────────────────────────────
    // createConnection
    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("createConnection")
    class CreateConnection {

        @Test
        @DisplayName("연결 생성 시 SseEmitter를 반환하고 온라인 상태로 설정한다")
        void success_returnsEmitterAndSetsOnline() {
            UUID userId = UUID.randomUUID();

            SseEmitter emitter = service.createConnection(userId, newConnectionId());

            assertThat(emitter).isNotNull();
            then(onlineStatusService).should().setUserOnline(userId);
        }

        @Test
        @DisplayName("같은 userId로 두 번째 탭을 열어도 첫 번째 emitter가 살아있다")
        void multiTab_bothEmittersAlive() {
            UUID userId = UUID.randomUUID();

            SseEmitter emitterA = service.createConnection(userId, newConnectionId());
            SseEmitter emitterB = service.createConnection(userId, newConnectionId());

            assertThat(emitterA).isNotNull();
            assertThat(emitterB).isNotNull();
            assertThat(emitterA).isNotSameAs(emitterB);
        }

        @Test
        @DisplayName("같은 userId로 두 탭이 연결돼도 setUserOnline은 최초 1회만 호출된다")
        void multiTab_setOnlineCalledOnce() {
            UUID userId = UUID.randomUUID();

            service.createConnection(userId, newConnectionId());
            service.createConnection(userId, newConnectionId());

            // 첫 번째 연결 시에만 온라인 전환
            then(onlineStatusService).should(times(1)).setUserOnline(userId);
        }

        @Test
        @DisplayName("서로 다른 userId는 각각 독립적인 emitter를 가진다")
        void differentUsers_independentEmitters() {
            UUID userA = UUID.randomUUID();
            UUID userB = UUID.randomUUID();

            SseEmitter emitterA = service.createConnection(userA, newConnectionId());
            SseEmitter emitterB = service.createConnection(userB, newConnectionId());

            assertThat(emitterA).isNotSameAs(emitterB);
            then(onlineStatusService).should().setUserOnline(userA);
            then(onlineStatusService).should().setUserOnline(userB);
        }
    }

    // ──────────────────────────────────────────────
    // disconnectUser
    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("disconnectUser")
    class DisconnectUser {

        @Test
        @DisplayName("연결된 사용자를 끊으면 오프라인 상태로 설정한다")
        void connected_setsOffline() {
            UUID userId = UUID.randomUUID();
            service.createConnection(userId, newConnectionId());

            service.disconnectUser(userId);

            then(onlineStatusService).should().setUserOffline(userId);
        }

        @Test
        @DisplayName("연결된 적 없는 userId도 오프라인 상태로 안전하게 설정한다")
        void notConnected_setsOfflineSafely() {
            UUID userId = UUID.randomUUID();

            service.disconnectUser(userId);

            then(onlineStatusService).should().setUserOffline(userId);
        }

        @Test
        @DisplayName("멀티탭 상태에서 disconnectUser 호출 시 모든 탭이 끊기고 오프라인 전환된다")
        void multiTab_disconnectUser_allTabsClosed() {
            UUID userId = UUID.randomUUID();
            service.createConnection(userId, newConnectionId());
            service.createConnection(userId, newConnectionId());

            service.disconnectUser(userId);

            then(onlineStatusService).should().setUserOffline(userId);
        }

        @Test
        @DisplayName("disconnect 후 재연결하면 새로운 emitter가 반환된다")
        void disconnectThenReconnect_returnsNewEmitter() {
            UUID userId = UUID.randomUUID();
            SseEmitter first = service.createConnection(userId, newConnectionId());
            service.disconnectUser(userId);

            SseEmitter second = service.createConnection(userId, newConnectionId());

            assertThat(first).isNotSameAs(second);
        }
    }

    // ──────────────────────────────────────────────
    // disconnectConnection
    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("disconnectConnection")
    class DisconnectConnection {

        @Test
        @DisplayName("탭 하나만 끊으면 나머지 탭은 유지되고 온라인 상태가 유지된다")
        void oneTab_remainsOnline() {
            UUID userId = UUID.randomUUID();
            String connIdA = newConnectionId();
            String connIdB = newConnectionId();
            service.createConnection(userId, connIdA);
            service.createConnection(userId, connIdB);

            service.disconnectConnection(userId, connIdA);

            // 아직 탭B가 남아있으므로 오프라인 전환 없음
            then(onlineStatusService).should(never()).setUserOffline(userId);
        }

        @Test
        @DisplayName("마지막 탭을 disconnectConnection으로 끊으면 오프라인으로 전환된다")
        void lastTab_setsOffline() {
            UUID userId = UUID.randomUUID();
            String connId = newConnectionId();
            service.createConnection(userId, connId);

            service.disconnectConnection(userId, connId);

            then(onlineStatusService).should().setUserOffline(userId);
        }

        @Test
        @DisplayName("존재하지 않는 connectionId로 끊어도 예외가 발생하지 않는다")
        void unknownConnectionId_noException() {
            UUID userId = UUID.randomUUID();
            service.createConnection(userId, newConnectionId());

            service.disconnectConnection(userId, "non-existent-id");

            // 예외 없이 통과하면 성공
        }
    }

    // ──────────────────────────────────────────────
    // sendNotification
    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("sendNotification")
    class SendNotification {

        @Test
        @DisplayName("온라인 사용자에게는 Redis로 메시지를 발행하고 true를 반환한다")
        void online_publishesToRedisAndReturnsTrue() {
            UUID userId = UUID.randomUUID();
            given(onlineStatusService.isUserOnline(userId)).willReturn(true);

            boolean result = service.sendNotification(userId, "제목", "내용", NotificationType.SCHEDULE, "id1");

            assertThat(result).isTrue();
            then(redisTemplate).should().convertAndSend(anyString(), any(NotificationMessage.class));
        }

        @Test
        @DisplayName("오프라인 사용자에게는 Redis 발행 없이 false를 반환한다")
        void offline_returnsFalseWithoutPublishing() {
            UUID userId = UUID.randomUUID();
            given(onlineStatusService.isUserOnline(userId)).willReturn(false);

            boolean result = service.sendNotification(userId, "제목", "내용", NotificationType.SCHEDULE, "id1");

            assertThat(result).isFalse();
            then(redisTemplate).should(never()).convertAndSend(anyString(), any());
        }

        @Test
        @DisplayName("Redis 발행 실패 시 NOTIFICATION_REDIS_MESSAGE_FAILED 예외를 던진다")
        void redisFails_throwsCustomException() {
            UUID userId = UUID.randomUUID();
            given(onlineStatusService.isUserOnline(userId)).willReturn(true);
            given(redisTemplate.convertAndSend(anyString(), any()))
                    .willThrow(new RuntimeException("Redis connection error"));

            assertThatThrownBy(() ->
                    service.sendNotification(userId, "제목", "내용", NotificationType.SCHEDULE, "id1"))
                    .isInstanceOf(CustomException.class)
                    .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.NOTIFICATION_REDIS_MESSAGE_FAILED));
        }
    }

    // ──────────────────────────────────────────────
    // onMessage
    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("onMessage")
    class OnMessage {

        @Test
        @DisplayName("연결된 사용자의 Redis 메시지는 예외 없이 처리된다")
        void connectedUser_processedWithoutException() throws Exception {
            UUID userId = UUID.randomUUID();
            service.createConnection(userId, newConnectionId());

            service.onMessage(buildRedisMessage(userId, NotificationType.SCHEDULE), null);
        }

        @Test
        @DisplayName("멀티탭 사용자에게 메시지가 오면 모든 탭에 전송 시도한다")
        void multiTab_messageDeliveredToAllTabs() throws Exception {
            UUID userId = UUID.randomUUID();
            service.createConnection(userId, newConnectionId());
            service.createConnection(userId, newConnectionId());

            // 예외 없이 두 탭 모두에 전송 시도되면 성공
            service.onMessage(buildRedisMessage(userId, NotificationType.SCHEDULE), null);
        }

        @Test
        @DisplayName("연결되지 않은 사용자의 메시지는 무시하고 오프라인 처리도 하지 않는다")
        void notConnectedUser_ignored() throws Exception {
            UUID userId = UUID.randomUUID();

            service.onMessage(buildRedisMessage(userId, NotificationType.PAYMENT), null);

            then(onlineStatusService).should(never()).setUserOffline(any());
        }

        @Test
        @DisplayName("잘못된 JSON이 오면 예외를 바깥으로 전파하지 않는다")
        void malformedJson_swallowsException() {
            Message redisMessage = mock(Message.class);
            given(redisMessage.getBody()).willReturn("invalid-json".getBytes());

            service.onMessage(redisMessage, null);
        }
    }

    // ──────────────────────────────────────────────
    // cleanupInactiveConnections
    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("cleanupInactiveConnections")
    class CleanupInactiveConnections {

        @Test
        @DisplayName("연결이 없으면 오프라인 처리 없이 통과한다")
        void noConnections_noop() {
            service.cleanupInactiveConnections();

            then(onlineStatusService).should(never()).setUserOffline(any());
        }

        @Test
        @DisplayName("연결이 있어도 예외 없이 완료된다")
        void withConnections_completesWithoutException() {
            service.createConnection(UUID.randomUUID(), newConnectionId());
            service.createConnection(UUID.randomUUID(), newConnectionId());

            service.cleanupInactiveConnections();
        }
    }

    // ──────────────────────────────────────────────
    // shutdown
    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("shutdown")
    class Shutdown {

        @Test
        @DisplayName("연결이 없어도 예외 없이 종료된다")
        void noConnections_shutdownCleanly() {
            service.shutdown();
        }

        @Test
        @DisplayName("멀티탭 연결이 있는 상태에서 shutdown하면 모든 연결을 정리한다")
        void withMultiTabConnections_cleansAllUp() {
            UUID userId = UUID.randomUUID();
            service.createConnection(userId, newConnectionId());
            service.createConnection(userId, newConnectionId());

            service.shutdown();
        }
    }

    // ──────────────────────────────────────────────
    // helpers
    // ──────────────────────────────────────────────

    private String newConnectionId() {
        return UUID.randomUUID().toString();
    }

    private Message buildRedisMessage(UUID userId, NotificationType type) throws Exception {
        NotificationMessage payload = new NotificationMessage(userId, "제목", "내용", type, "relatedId");
        byte[] body = objectMapper.writeValueAsBytes(payload);

        Message msg = mock(Message.class);
        given(msg.getBody()).willReturn(body);
        return msg;
    }
}
