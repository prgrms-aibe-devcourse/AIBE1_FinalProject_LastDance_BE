package store.lastdance.service.notification;

import java.util.UUID;

public interface OnlineStatusService {
    void setUserOnline(UUID userId);
    void setUserOffline(UUID userId);
    boolean isUserOnline(UUID userId);
    void refreshOnlineTTL(UUID userId);
}
