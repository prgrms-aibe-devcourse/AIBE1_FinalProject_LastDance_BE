package store.lastdance.service.common;

import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import store.lastdance.exception.CustomException;
import store.lastdance.exception.ErrorCode;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Service
public class FileValidationServiceImpl implements FileValidationService{
        private static final Tika tika = new Tika();
        private static final List<String> ALLOWED_FILE_TYPES = List.of(
                "image/jpeg",
                "image/png",
                "image/jpg",
                "image/gif",
                "image/webp"
        );
    @Override
    public void validateImageFile(org.springframework.web.multipart.MultipartFile file) {
        if(file == null || file.isEmpty()) {
            return;
        }

        try (InputStream inputStream = file.getInputStream()) {
            String mimeType = tika.detect(inputStream);

            if(!ALLOWED_FILE_TYPES.contains(mimeType)) {
                throw new CustomException(ErrorCode.INVALID_FILE_TYPE);
            }
        } catch (IOException e) {
            throw new CustomException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }
}
