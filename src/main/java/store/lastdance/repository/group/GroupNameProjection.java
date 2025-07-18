package store.lastdance.repository.group;

import java.util.UUID;

public interface GroupNameProjection {
    UUID getGroupId();
    String getGroupName();
}
