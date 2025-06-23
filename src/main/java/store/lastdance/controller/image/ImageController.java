package store.lastdance.controller.image;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import store.lastdance.domain.common.ImageFile;
import store.lastdance.dto.response.ApiResponse;
import store.lastdance.service.image.ImageService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/images")
@RequiredArgsConstructor
@Slf4j
public class ImageController {

    private final ImageService imageService;

    @GetMapping("/{fileId}")
    public ResponseEntity<byte[]> getImage(@PathVariable UUID fileId) {
        try {
            byte[] imageBytes = imageService.getImageBytes(fileId);
            ImageFile imageFile = imageService.getImageFile(fileId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(imageFile.getMimeType()));
            headers.setContentLength(imageBytes.length);
            headers.setCacheControl("max-age=3600"); // 1시간 캐싱

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(imageBytes);

        } catch (Exception e) {
            log.error("이미지 조회 실패: fileId={}, error={}", fileId, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
}
