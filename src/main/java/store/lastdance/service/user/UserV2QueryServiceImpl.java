package store.lastdance.service.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import store.lastdance.converter.user.UserConverter;
import store.lastdance.domain.user.User;
import store.lastdance.dto.user.UserResponseDTO;
import store.lastdance.exception.CustomException;
import store.lastdance.exception.ErrorCode;
import store.lastdance.repository.user.UserRepository;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserV2QueryServiceImpl implements UserV2QueryService {

    private final UserRepository userRepository;
    private final UserConverter userConverter;

    @Override
    public User findByActiveUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (!user.getIsActive()) {
            throw new CustomException(ErrorCode.USER_INACTIVE);
        }

        return user;
    }

    @Override
    public User findByUserId(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    @Override
    public UserResponseDTO getUserWithProfileImage(UUID userId) {
        User user = userRepository.findByIdWithProfileImage(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        return userConverter.toResponseDTO(user);
    }

    @Override
    public boolean isNicknameAvailable(UUID userId, String nickname) {
        if (nickname == null || nickname.trim().isEmpty()) {
            return false;
        }
        return !userRepository.existsByNicknameAndUserIdNot(nickname.trim(), userId);
    }

    @Override
    public void validateUserExists(UUID userId) {
        if (!userRepository.existsById(userId)) {
            log.error("User with ID {} does not exist", userId);
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }

        if (!userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND))
                .getIsActive()) {
            throw new CustomException(ErrorCode.USER_INACTIVE);
        }

        log.info("User with ID {} exists", userId);
    }
}
