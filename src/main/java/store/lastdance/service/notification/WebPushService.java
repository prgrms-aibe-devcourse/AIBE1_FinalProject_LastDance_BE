package store.lastdance.service.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import store.lastdance.domain.notification.NotificationSetting;
import store.lastdance.domain.notification.NotificationType;
import store.lastdance.repository.notification.NotificationSettingRepository;

import jakarta.annotation.PostConstruct;
import java.security.GeneralSecurityException;
import java.security.Security;
import java.util.Map;
import java.util.UUID;

@Service
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
        }

        if (publicKey.isEmpty() || privateKey.isEmpty()) {
            return;
        }

        try {
            pushService = new PushService();
            pushService.setSubject(subject);
            pushService.setPublicKey(publicKey);
            pushService.setPrivateKey(privateKey);

        } catch (Exception e) {
            throw e;
        }
    }

    public boolean sendNotification(UUID userId, String title, String content, NotificationType type, String relatedId) {
        if (pushService == null) {
            return false;
        }

        NotificationSetting setting = settingRepository.findByUserId(userId).orElse(null);
        if (setting == null || !setting.isWebPushAvailable()) {
            return false;
        }

        try {
            Map<String, Object> payload = Map.of(
                    "title", title,
                    "body", content,
                    "icon", "/icons/" + type.name().toLowerCase() + ".png",
                    "badge", "/badge.png",
                    "data", Map.of(
                            "type", type.name(), 
                            "userId", userId.toString(),
                            "relatedId", relatedId != null ? relatedId : ""
                    )
            );

            // 구독 정보 그대로 사용 (변환하지 않음)
            String endpoint = setting.getWebpushEndpoint();
            String p256dh = setting.getWebpushP256dh();
            String auth = setting.getWebpushAuth();


            Notification notification = new Notification(
                    endpoint,
                    p256dh,
                    auth,
                    objectMapper.writeValueAsString(payload)
            );

            HttpResponse response = pushService.send(notification);
            int statusCode = response.getStatusLine().getStatusCode();
            String responseBody = "";
            
            // 응답 본문 읽기 (오류 디버깅용)
            try {
                if (response.getEntity() != null) {
                    responseBody = EntityUtils.toString(response.getEntity());
                }
            } catch (Exception e) {
            }

            if (statusCode == 200 || statusCode == 201) {
                return true;
            } else if (statusCode == 410) {
                // 구독 만료 - 제거
                setting.removeWebPushSubscription();
                settingRepository.save(setting);
            }

            return false;

        } catch (Exception e) {
            return false;
        }
    }

    public void subscribeUser(UUID userId, String endpoint, String p256dh, String auth) {
        try {
            NotificationSetting setting = settingRepository.findByUserId(userId)
                    .orElseGet(() -> {
                        NotificationSetting newSetting = NotificationSetting.builder()
                                .userId(userId)
                                .build();
                        return settingRepository.save(newSetting);
                    });

            setting.updateWebPushSubscription(endpoint, p256dh, auth);
            settingRepository.save(setting);

        } catch (Exception e) {
            throw e;
        }
    }

    public void unsubscribeUser(UUID userId) {
        NotificationSetting setting = settingRepository.findByUserId(userId).orElse(null);
        if (setting != null) {
            setting.removeWebPushSubscription();
            settingRepository.save(setting);
        }
    }

    public boolean hasSubscription(UUID userId) {
        return settingRepository.findByUserId(userId)
                .map(setting -> setting.hasWebPushSubscription() &&
                        setting.getWebpushEnabled() != null &&
                        setting.getWebpushEnabled())
                .orElse(false);
    }

    // VAPID 키 검증 메서드 추가
    public boolean isVapidConfigured() {
        return pushService != null && !publicKey.isEmpty() && !privateKey.isEmpty();
    }
}
