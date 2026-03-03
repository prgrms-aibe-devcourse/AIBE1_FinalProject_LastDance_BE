package store.lastdance.converter.user;

import org.springframework.stereotype.Component;
import store.lastdance.domain.user.User;
import store.lastdance.dto.user.UserResponseDTO;

@Component
public class UserConverter {

    public UserResponseDTO toResponseDTO(User user) {
        if (user == null) {
            return null;
        }

        String profileImageUrl = (user.getProfileImageFile() != null)
                ? user.getProfileImageFile().getFilePath()
                : null;

        return new UserResponseDTO(
                user.getUserId(),
                user.getEmail(),
                user.getUsername(),
                user.getNickname(),
                profileImageUrl,
                user.getProvider().name(),
                user.getIsActive(),
                user.getIsBanned(),
                user.getUserBudget()
        );
    }
}
