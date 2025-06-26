package store.lastdance.service.image;

import org.springframework.web.multipart.MultipartFile;
import store.lastdance.domain.common.ImageFile;

import java.util.UUID;

public interface ImageService {
    ImageFile uploadImageToS3(MultipartFile file);

    void deleteImageFromS3(UUID fileId);

    ImageFile getImageFile(UUID fileId);

    String getImageURL(UUID fileId);

}
