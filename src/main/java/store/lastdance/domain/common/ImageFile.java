package store.lastdance.domain.common;

import lombok.*;
import jakarta.persistence.*;

import java.util.UUID;

@Getter
@Entity
@Table(name = "image_files")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ImageFile extends BaseTimeEntity {
    @Id
    @Column(name = "file_id")
    private UUID fileId;

    @Column(name = "original_name", nullable = false, length = 255)
    private String originalName;

    @Column(name = "stored_name", nullable = false, length = 255)
    private String storedName;

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "mime_type", nullable = false, length = 100)
    private String mimeType;

    @Builder
    public ImageFile(@NonNull UUID fileId, @NonNull String originalName, @NonNull String storedName,
                     @NonNull String filePath, @NonNull Long fileSize, @NonNull String mimeType) {
        this.fileId = fileId;
        this.originalName = originalName;
        this.storedName = storedName;
        this.filePath = filePath;
        this.fileSize = fileSize;
        this.mimeType = mimeType;
    }

    public void updateFilePath(String newFilePath) {
        this.filePath = newFilePath;
    }


    public String getFileUrl() {
        String baseUrl = "https://lastdance-s3-bucket.s3.ap-northeast-2.amazonaws.com/";

        if (this.filePath.startsWith("http://") || this.filePath.startsWith("https://")) {
            return this.filePath;
        }
        if (!baseUrl.endsWith("/") && !this.filePath.startsWith("/")) {
            return baseUrl + "/" + this.filePath;
        }
        if (baseUrl.endsWith("/") && this.filePath.startsWith("/")) {
            return baseUrl + this.filePath.substring(1);
        }
        return baseUrl + this.filePath;
    }
}

