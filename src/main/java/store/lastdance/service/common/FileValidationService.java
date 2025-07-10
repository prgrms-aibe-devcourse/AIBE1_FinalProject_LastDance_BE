package store.lastdance.service.common;

import org.springframework.web.multipart.MultipartFile;

public interface FileValidationService {
    public void validateImageFile(MultipartFile file);

}
