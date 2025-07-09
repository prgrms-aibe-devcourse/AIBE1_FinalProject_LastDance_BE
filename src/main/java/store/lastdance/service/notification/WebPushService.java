package store.lastdance.service.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.apache.http.HttpResponse;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import store.lastdance.domain.notification.NotificationSetting;
import store.lastdance.domain.notification.NotificationType;
import store.lastdance.repository.notification.NotificationSettingRepository;

import jakarta.annotation.PostConstruct;
import java.security.GeneralSecurityException;
import java.security.Security;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class WebPushService {

    private final NotificationSettingRepository settingRepository;
    private final ObjectMapper objectMapper;
    private PushService pushService;

    @Value("${webpush.vapid.public-key:}")
    private String publicKey;

    @Value("${webpush.vapid.private-key:}")
    private String privateKey;

    @Value("${webpush.vapid.subject:mailto:admin@lastdance.com}")
    private String subject;

    @PostConstruct
    public void init() throws GeneralSecurityException {
        // Bouncy Castle 프로바이더 등록
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
            log.info("Bouncy Castle 프로바이더 등록 완료");
        }

        if (publicKey.isEmpty() || privateKey.isEmpty()) {
            log.warn("VAPID 키가 설정되지 않음. 웹푸시 기능 비활성화");
            return;
        }

        try {
            pushService = new PushService();
            pushService.setSubject(subject);
            pushService.setPublicKey(publicKey);
            pushService.setPrivateKey(privateKey);

            log.info("웹푸시 서비스 초기화 완료");
        } catch (Exception e) {
            log.error("웹푸시 서비스 초기화 실패: {}", e.getMessage(), e);
            throw e;
        }
    }

    public boolean sendNotification(UUID userId, String title, String content, NotificationType type, String relatedId) {
        if (pushService == null) {
            log.debug("웹푸시 서비스가 초기화되지 않음");
            return false;
        }

        NotificationSetting setting = settingRepository.findByUserId(userId).orElse(null);
        if (setting == null || !setting.isWebPushAvailable()) {
            log.debug("웹푸시 구독 정보 없음: userId={}", userId);
            return false;
        }

        try {
            Map<String, Object> payload = Map.of(
                    "title", title,
                    "body", content,
                    "icon", "/icons/" + type.name().toLowerCase() + ".png",
                    "badge", "/badge.png",
                    "data", Map.of("type", type.name(), "userId", userId.toString()),
                    "relatedId", relatedId
            );

            // Base64 URL-safe 키를 표준 Base64로 변환
            String p256dh = setting.getWebpushP256dh();
            String auth = setting.getWebpushAuth();

            if (p256dh != null && (p256dh.contains("-") || p256dh.contains("_"))) {
                // URL-safe Base64를 일반 Base64로 변환
                byte[] p256dhBytes = Base64.getUrlDecoder().decode(p256dh);
                p256dh = Base64.getEncoder().encodeToString(p256dhBytes);
            }

            if (auth != null && (auth.contains("-") || auth.contains("_"))) {
                // URL-safe Base64를 일반 Base64로 변환
                byte[] authBytes = Base64.getUrlDecoder().decode(auth);
                auth = Base64.getEncoder().encodeToString(authBytes);
            }

            Notification notification = new Notification(
                    setting.getWebpushEndpoint(),
                    p256dh,
                    auth,
                    objectMapper.writeValueAsString(payload)
            );

            HttpResponse response = pushService.send(notification);
            int statusCode = response.getStatusLine().getStatusCode();

            if (statusCode == 200 || statusCode == 201) {
                log.info("웹푸시 전송 성공: userId={}, type={}", userId, type);
                return true;
            } else if (statusCode == 410) {
                // 구독 만료 - 제거
                setting.removeWebPushSubscription();
                settingRepository.save(setting);
                log.warn("웹푸시 구독 만료로 제거: userId={}", userId);
            }

            return false;

        } catch (Exception e) {
            log.error("웹푸시 전송 실패: userId={}, error={}", userId, e.getMessage());
            return false;
        }
    }

    public void subscribeUser(UUID userId, String endpoint, String p256dh, String auth) {
        NotificationSetting setting = settingRepository.findByUserId(userId)
                .orElseGet(() -> {
                    NotificationSetting newSetting = NotificationSetting.builder()
                            .userId(userId)
                            .build();
                    return settingRepository.save(newSetting);
                });

        setting.updateWebPushSubscription(endpoint, p256dh, auth);
        settingRepository.save(setting);

        log.info("웹푸시 구독 등록: userId={}", userId);
    }

    public void unsubscribeUser(UUID userId) {
        NotificationSetting setting = settingRepository.findByUserId(userId).orElse(null);
        if (setting != null) {
            setting.removeWebPushSubscription();
            settingRepository.save(setting);
            log.info("웹푸시 구독 해제: userId={}", userId);
        }
    }

    public boolean hasSubscription(UUID userId) {
        return settingRepository.findByUserId(userId)
                .map(setting -> setting.hasWebPushSubscription() &&
                        setting.getWebpushEnabled() != null &&
                        setting.getWebpushEnabled())
                .orElse(false);
    }
}