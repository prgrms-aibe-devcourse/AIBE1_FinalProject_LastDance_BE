package store.lastdance.dto.group;

import java.util.UUID;

public record GroupApplicationRequestDTO (UUID groupId, UUID userId) {
}