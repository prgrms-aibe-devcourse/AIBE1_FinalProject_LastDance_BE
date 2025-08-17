package store.lastdance.repository.prompt;

import org.springframework.data.jpa.repository.JpaRepository;
import store.lastdance.domain.prompt.Prompt;

import java.util.Optional;

public interface PromptRepository extends JpaRepository<Prompt, Long> {
    Optional<Prompt> findByPromptType(String promptType);
}
