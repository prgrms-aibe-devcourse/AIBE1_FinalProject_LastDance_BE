package store.lastdance.service.image;

import io.awspring.cloud.s3.S3Operations;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import store.lastdance.domain.common.ImageFile;
import store.lastdance.exception.CustomException;
import store.lastdance.exception.ErrorCode;
import store.lastdance.repository.common.ImageFileRepository;
import store.lastdance.service.common.FileValidationService;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImageServiceImpl implements ImageService {

    private final ImageFileRepository imageFileRepository;
    private final S3Operations s3Operations;
    private final S3Presigner s3Presigner;
    private final FileValidationService fileValidationService;

    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucketName;

    @Override
    @Transactional
    public ImageFile uploadImageToS3(MultipartFile file, String folder, int maxSize) {
        validateImageFile(file, maxSize);

        try {
            UUID fileId = UUID.randomUUID();
            String originalFilename = file.getOriginalFilename();
            String extension = getFileExtension(originalFilename);
            String storedName = "%s/%s%s".formatted(folder, fileId, extension);

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
    public String generatePresignedUrl(UUID fileId) {
        ImageFile imageFile = getImageFile(fileId);
        return generatePresignedUrl(imageFile);
    }

    @Override
    public String generatePresignedUrl(ImageFile imageFile) {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(imageFile.getStoredName())
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofHours(1)) // 1시간 유효
                    .getObjectRequest(getObjectRequest)
                    .build();

            return s3Presigner.presignGetObject(presignRequest).url().toString();
        } catch (Exception e) {
            log.error("Pre-signed URL 생성 실패: {}", e.getMessage());
            throw new CustomException(ErrorCode.FILE_DOWNLOAD_FAILED);
        }
    }

    private ImageFile getImageFile(UUID fileId) {
        return imageFileRepository.findById(fileId).orElseThrow(() -> new CustomException(ErrorCode.FILE_NOT_FOUND));
    }

    private String getFileExtension(String filename) {
        return filename.substring(filename.lastIndexOf("."));
    }

    private void validateImageFile(MultipartFile file, int maxSize) {
        if (file.isEmpty()) {
            throw new CustomException(ErrorCode.EMPTY_FILE);
        }

        if (file.getSize() > maxSize) {
            throw new CustomException(ErrorCode.FILE_SIZE_EXCEEDED);
        }

        fileValidationService.validateImageFile(file);
    }
    
}
