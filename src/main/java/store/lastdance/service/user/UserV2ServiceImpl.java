package store.lastdance.service.user;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import store.lastdance.converter.user.UserConverter;
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
public class UserV2ServiceImpl implements UserV2Service {

    private final UserRepository userRepository;
    private final ImageService imageService;
    private final ApplicationEventPublisher eventPublisher;
    private final UserV2QueryService userV2QueryService;
    private final UserConverter userConverter;

    @Override
    @Transactional
    @CacheEvict(value = "nickname", allEntries = true)
    public User updateMyInfo(UUID userId, UserUpdateRequestDTO requestDTO) {
        User user = userV2QueryService.findByActiveUser(userId);

        if (requestDTO.nickname() != null && !requestDTO.nickname().trim().isEmpty()) {
            String newNickname = requestDTO.nickname().trim();

            if (!userV2QueryService.isNicknameAvailable(userId, newNickname)) {
                throw new CustomException(ErrorCode.NICKNAME_ALREADY_EXISTS);
            }

            user.updateNickname(newNickname);
        }

        if (requestDTO.monthlyBudget() != null) {
            user.updateBudget(requestDTO.monthlyBudget());
        }

        return userRepository.save(user);
    }

    @Override
    @Transactional
    @CacheEvict(value = "userProfile", key = "#userId")
    public UserResponseDTO updateProfileImage(UUID userId, MultipartFile file) {
        User user = userRepository.findByIdWithProfileImage(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        UUID oldImageFileId = user.getProfileImageFile() != null ? user.getProfileImageFile().getFileId() : null;
        UUID newImageFileId = null;

        try {
            ImageFile newImageFile = imageService.uploadImageToS3(file, "profile-image", 5 * 1024 * 1024);
            newImageFileId = newImageFile.getFileId();
            user.updateProfileImage(newImageFile);

            userRepository.save(user);

            if (oldImageFileId != null) {
                imageService.deleteImageFromS3(oldImageFileId);
            }

        } catch (Exception e) {
            if (newImageFileId != null) {
                try {
                    imageService.deleteImageFromS3(newImageFileId);
                } catch (Exception deleteEx) {
                    log.error("고아파일 정리 실패: {}", deleteEx.getMessage());
                }
            }
            throw e;
        }

        return userConverter.toResponseDTO(user);
    }

    @Override
    @Transactional
    @CacheEvict(value = "userProfile", key = "#userId")
    public UserResponseDTO deleteProfileImage(UUID userId) {
        User user = userRepository.findByIdWithProfileImage(userId).orElseThrow(
                () -> new CustomException(ErrorCode.USER_NOT_FOUND)
        );

        if (user.getProfileImageFile() != null) {
            imageService.deleteImageFromS3(user.getProfileImageFile().getFileId());
            user.removeProfileImage();
        }

        userRepository.save(user);
        return userConverter.toResponseDTO(user);
    }


    @Override
    @Transactional
    public void deactivateUser(UUID userId, HttpServletRequest request, HttpServletResponse response) {
        User user = userV2QueryService.findByActiveUser(userId);
        log.info("사용자 계정 삭제(비활성화 처리): userId={}", userId);

        String deletedSuffix = "deleted_%s".formatted(userId.toString());
        user.updateEmail(deletedSuffix + "@lastdance.store");
        user.updateProviderId(deletedSuffix);
        user.updateNickname(deletedSuffix);

        user.deactivate();
        eventPublisher.publishEvent(new UserDeactivatedEvent(this, userId, request, response));

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
        log.info("계정 삭제(비활성화 완료): userId={}", userId);
    }

}
