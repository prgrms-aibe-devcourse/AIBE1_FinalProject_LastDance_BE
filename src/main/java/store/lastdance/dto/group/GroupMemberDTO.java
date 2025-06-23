package store.lastdance.dto.group;

import store.lastdance.domain.common.ImageFile;
import store.lastdance.domain.group.GroupRole;
import store.lastdance.domain.user.UserRole;

import java.util.UUID;

public record GroupMemberDTO(UUID userId,
                             String nickname,
                             String profileImagePath,
                             GroupRole role) {
}
