package store.lastdance.service.onlinestatus;

import java.util.UUID;

public interface OnlineStatusService {
    void setUserOnline(UUID userId);
    void setUserOffline(UUID userId);
    boolean isUserOnline(UUID userId);
}
