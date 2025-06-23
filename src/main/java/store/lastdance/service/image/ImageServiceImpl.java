package store.lastdance.service.image;

import io.awspring.cloud.s3.S3Operations;
import io.awspring.cloud.s3.S3Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import store.lastdance.domain.common.ImageFile;
import store.lastdance.exception.CustomException;
import store.lastdance.exception.ErrorCode;
import store.lastdance.repository.common.ImageFileRepository;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImageServiceImpl implements ImageService {

    private final ImageFileRepository imageFileRepository;
    private final S3Operations s3Operations;
    private final S3Presigner s3Presigner;
    private final int FILE_MAX_SIZE = 5 * 1024 * 1024;

    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucketName;

    @Override
    @Transactional
    public ImageFile uploadImageToS3(MultipartFile file) {
        validateImageFile(file);

        try {
            UUID fileId = UUID.randomUUID();
            String originalFilename = file.getOriginalFilename();
            String extension = getFileExtension(originalFilename);
            String storedName = "profile-image/%s%s".formatted(fileId, extension);

            s3Operations.upload(bucketName, storedName, file.getInputStream());

            String s3Url = String.format("https://%s.s3.ap-northeast-2.amazonaws.com/%s", bucketName, storedName);

            ImageFile imageFile = ImageFile.builder()
                    .fileId(fileId)
                    .originalName(originalFilename)
                    .storedName(storedName)
                    .filePath(s3Url)
                    .fileSize(file.getSize())
                    .mimeType(file.getContentType())
                    .build();

            return imageFileRepository.save(imageFile);

        } catch (Exception e) {
            log.error("S3 파일 업로드 실패: {}", e.getMessage());
            throw new CustomException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    @Override
    @Transactional
    public void deleteImageFromS3(UUID fileId) {
        ImageFile imageFile = imageFileRepository.findById(fileId).orElseThrow(
                () -> new CustomException(ErrorCode.FILE_NOT_FOUND)
        );

        try {
            s3Operations.deleteObject(bucketName, imageFile.getStoredName());
            imageFileRepository.delete(imageFile);
        } catch (Exception e) {
            log.error("S3 파일 삭제 실패: {}", e.getMessage());
            throw new CustomException(ErrorCode.FILE_DELETE_FAILED);
        }
    }

    @Override
    public ImageFile getImageFile(UUID fileId) {
        return imageFileRepository.findById(fileId).orElseThrow(() -> new CustomException(ErrorCode.FILE_NOT_FOUND));
    }

    @Override
    public String getImageURL(UUID fileId) {
        ImageFile imageFile = getImageFile(fileId);
        return imageFile.getFilePath();
    }

    @Override
    public byte[] getImageBytes(UUID fileId) {
        ImageFile imageFile = getImageFile(fileId);
        
        try {
            S3Resource download = s3Operations.download(bucketName, imageFile.getStoredName());
            return download.getContentAsByteArray();
        } catch (Exception e) {
            log.error("S3 파일 다운로드 실패: {}", e.getMessage());
            throw new CustomException(ErrorCode.FILE_DOWNLOAD_FAILED);
        }
    }

    private String getFileExtension(String filename) {
        return filename.substring(filename.lastIndexOf("."));
    }

    private void validateImageFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new CustomException(ErrorCode.EMPTY_FILE);
        }

        if (file.getSize() > FILE_MAX_SIZE) {
            throw new CustomException(ErrorCode.FILE_SIZE_EXCEEDED);
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new CustomException(ErrorCode.INVALID_FILE_TYPE);
        }

        String filename = file.getOriginalFilename();
        if (filename == null || !isAllowedExtensions(filename)) {
            throw new CustomException(ErrorCode.INVALID_FILE_EXTENSION);
        }
    }

    private boolean isAllowedExtensions(String filename) {
        String extension = getFileExtension(filename).toLowerCase();
        return extension.equals(".jpg") || extension.equals(".jpeg") ||
                extension.equals(".png") || extension.equals(".gif");
    }

    
}
