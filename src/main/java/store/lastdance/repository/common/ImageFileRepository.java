package store.lastdance.repository.common;

import org.springframework.data.jpa.repository.JpaRepository;
import store.lastdance.domain.common.ImageFile;

import java.util.Optional;
import java.util.UUID;

public interface ImageFileRepository extends JpaRepository<ImageFile, UUID> {
    Optional<ImageFile> findByStoredName(String storedName);
}
