package store.lastdance.dto.admin;

import java.util.UUID;

public record AdminPageGroupDTO(
        UUID groupId,
        String groupName
) {
}
