package store.lastdance.service.user;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import store.lastdance.domain.common.ImageFile;
import store.lastdance.domain.user.User;
import store.lastdance.dto.user.UserResponseDTO;
import store.lastdance.dto.user.UserUpdateRequestDTO;
import store.lastdance.event.UserDeactivatedEvent;
import store.lastdance.exception.CustomException;
import store.lastdance.exception.ErrorCode;
import store.lastdance.repository.user.UserRepository;
import store.lastdance.service.image.ImageService;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final ImageService imageService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public User findByActiveUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (!user.getIsActive()) {
            throw new CustomException(ErrorCode.USER_INACTIVE);
        }

        return user;
    }

    public User findByUserId(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    public UserResponseDTO getUserWithProfileImage(UUID userId) {
        User user = userRepository.findByIdWithProfileImage(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        return UserResponseDTO.from(user);
    }

    @Override
    @Transactional
    public User updateMyInfo(UUID userId, UserUpdateRequestDTO requestDTO) {
        User user = findByActiveUser(userId);

        // 닉네임 수정
        if (requestDTO.nickname() != null && !requestDTO.nickname().trim().isEmpty()) {
            String newNickname = requestDTO.nickname().trim();

            // 닉네임 중복체크
            if (!isNicknameAvailable(userId, newNickname)) {
                throw new CustomException(ErrorCode.NICKNAME_ALREADY_EXISTS);
            }

            user.updateNickname(newNickname);
        }

        // 예산 수정
        if (requestDTO.monthlyBudget() != null) {
            user.updateBudget(requestDTO.monthlyBudget());
        }

        return userRepository.save(user);
    }

    @Override
    @Transactional
    public UserResponseDTO updateProfileImage(UUID userId, MultipartFile file) {
        User user = userRepository.findByIdWithProfileImage(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 기존 이미지 삭제
        if (user.getProfileImageFile() != null) {
            imageService.deleteImageFromS3(user.getProfileImageFile().getFileId());
        }

        // 새 이미지 업로드
        ImageFile newImageFile = imageService.uploadImageToS3(file);
        user.updateProfileImage(newImageFile);

        userRepository.save(user);
        return UserResponseDTO.from(user);
    }

    @Override
    @Transactional
    public UserResponseDTO deleteProfileImage(UUID userid) {
        User user = userRepository.findByIdWithProfileImage(userid).orElseThrow(
                () -> new CustomException(ErrorCode.USER_NOT_FOUND)
        );

        // 기존 이미지 있으면 S3에서 삭제
        if (user.getProfileImageFile() != null) {
            imageService.deleteImageFromS3(user.getProfileImageFile().getFileId());
            user.removeProfileImage();
        }

        userRepository.save(user);
        return UserResponseDTO.from(user);
    }

    @Override
    public boolean isNicknameAvailable(UUID userId, String nickname) {
        if (nickname == null || nickname.trim().isEmpty()) {
            return false;
        }
        return !userRepository.existsByNicknameAndUserIdNot(nickname.trim(), userId);
    }

    @Override
    @Transactional
    public void deactivateUser(UUID userId, HttpServletRequest request, HttpServletResponse response) {
        User user = findByActiveUser(userId);
        log.info("사용자 계정 비활성화 처리: userId={}", userId);

        user.deactivate();
        eventPublisher.publishEvent(new UserDeactivatedEvent(this, userId, request, response));

        // 프로필 이미지 삭제 처리
        if (user.getProfileImageFile() != null) {
            try {
                imageService.deleteImageFromS3(user.getProfileImageFile().getFileId());
                user.removeProfileImage();
                log.info("프로필 이미지 삭제 완료: userId={}", userId);
            } catch (Exception e) {
                log.warn("프로필 이미지 삭제 중 오류 발생: userId={}, error={}", userId, e.getMessage());
            }
        }
        userRepository.save(user);
        log.info("계정 비활성화 완료: userId={}", userId);
    }
}
