package store.lastdance.service.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import store.lastdance.domain.user.User;
import store.lastdance.exception.CustomException;
import store.lastdance.exception.ErrorCode;
import store.lastdance.exception.UserNotFoundException;
import store.lastdance.repository.user.UserRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public User findByActiveUser(UUID userId) {
        return userRepository.findById(userId)
                .filter(User::getIsActive)
                .orElseThrow(UserNotFoundException::new);
    }

    public User findByUserId(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

}
