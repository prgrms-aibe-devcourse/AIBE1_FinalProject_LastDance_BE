package store.lastdance.service.prompt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import store.lastdance.domain.prompt.Prompt;
import store.lastdance.exception.CustomException;
import store.lastdance.exception.ErrorCode;
import store.lastdance.repository.prompt.PromptRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class PromptService {
    private final PromptRepository promptRepository;

    @Cacheable(value = "prompts", key = "#promptType")
    public String getPromptContent(String promptType) {
        return promptRepository.findByPromptType(promptType)
                .map(Prompt::getPromptContent)
                .orElseThrow(() -> new CustomException(ErrorCode.PROMPT_NOT_FOUND));
    }
}
