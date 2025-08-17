package store.lastdance.service.user;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import store.lastdance.converter.UserConverter;
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

    public User findByUserId(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    public UserResponseDTO getUserWithProfileImage(UUID userId) {
        User user = userRepository.findByIdWithProfileImage(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        return userConverter.toResponseDTO(user);
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

        UUID oldImageFileId = user.getProfileImageFile() != null ? user.getProfileImageFile().getFileId() : null;
        UUID newImageFileId = null;

        try {
            // 새 이미지 업로드
            ImageFile newImageFile = imageService.uploadImageToS3(file, "profile-image", 5 * 1024 * 1024);
            newImageFileId = newImageFile.getFileId();
            user.updateProfileImage(newImageFile);

            // DB 저장
            userRepository.save(user);

            // 기존 이미지 삭제 (새 이미지 저장 성공 후)
            if (oldImageFileId != null) {
                imageService.deleteImageFromS3(oldImageFileId);
            }

        } catch (Exception e) {
            // 새로 업로드한 파일 정리
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
    @Transactional
    public void deactivateUser(UUID userId, HttpServletRequest request, HttpServletResponse response) {
        User user = findByActiveUser(userId);
        log.info("사용자 계정 삭제(비활성화 처리): userId={}", userId);

        // OAuth 정보 및 이메일 마스킹으로 재가입 허용
        String deletedSuffix = "deleted_%s".formatted(userId.toString());
        user.updateEmail(deletedSuffix + "@lastdance.store");
        user.updateProviderId(deletedSuffix);
        user.updateNickname(deletedSuffix);

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
        log.info("계정 삭제(비활성화 완료): userId={}", userId);
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
