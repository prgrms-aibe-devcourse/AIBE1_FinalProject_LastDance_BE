package store.lastdance.domain.admin;

import lombok.*;
import jakarta.persistence.*;
import store.lastdance.domain.common.BaseTimeEntity;
import java.util.UUID;

@Getter
@Entity
@Table(name = "error_logs")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ErrorLog extends BaseTimeEntity {
    @Id
    @Column(name = "log_id")
    private UUID logId;

    @Column(name = "error_level", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ErrorLevel errorLevel;

    @Column(name = "error_message", nullable = false, columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "request_uri", length = 500)
    private String requestUri;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Builder
    public ErrorLog(@NonNull UUID logId, @NonNull ErrorLevel errorLevel, @NonNull String errorMessage,
                    String requestUri, String userAgent, String ipAddress) {
        this.logId = logId;
        this.errorLevel = errorLevel;
        this.errorMessage = errorMessage;
        this.requestUri = requestUri;
        this.userAgent = userAgent;
        this.ipAddress = ipAddress;
    }

    public static ErrorLog createErrorLog(ErrorLevel errorLevel, String errorMessage, String requestUri, String userAgent, String ipAddress) {
        return ErrorLog.builder()
                .logId(UUID.randomUUID())
                .errorLevel(errorLevel)
                .errorMessage(errorMessage)
                .requestUri(requestUri)
                .userAgent(userAgent)
                .ipAddress(ipAddress)
                .build();
    }
}
