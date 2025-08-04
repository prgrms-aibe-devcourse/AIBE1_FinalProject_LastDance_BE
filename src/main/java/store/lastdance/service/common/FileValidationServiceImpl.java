package store.lastdance.service.common;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import store.lastdance.exception.CustomException;
import store.lastdance.exception.ErrorCode;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Service
@Slf4j
public class FileValidationServiceImpl implements FileValidationService{
        
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

        try {
            String mimeType = file.getContentType();
            log.info("Detected MIME type: {} for file: {}", mimeType, file.getOriginalFilename());

            if(!ALLOWED_FILE_TYPES.contains(mimeType)) {
                log.warn("Attempt to upload an invalid file type. Detected: {}, Filename: {}", mimeType, file.getOriginalFilename());
                throw new CustomException(ErrorCode.INVALID_FILE_TYPE);
            }
        } catch (Exception e) {
            log.error("File validation failed for file: {}", file.getOriginalFilename(), e);
            throw new CustomException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }
}
