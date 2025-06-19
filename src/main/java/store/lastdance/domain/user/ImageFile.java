package store.lastdance.domain.user;

import lombok.*;
import jakarta.persistence.*;
import store.lastdance.domain.common.BaseTimeEntity;
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
}
