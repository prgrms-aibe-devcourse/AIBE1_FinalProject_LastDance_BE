package store.lastdance.service.image;

import org.springframework.web.multipart.MultipartFile;
import store.lastdance.domain.common.ImageFile;

import java.util.UUID;

public interface ImageService {
    ImageFile uploadImageToS3(MultipartFile file, String folder, int maxSize);

    void deleteImageFromS3(UUID fileId);

    String generatePresignedUrl(UUID fileId);

}
