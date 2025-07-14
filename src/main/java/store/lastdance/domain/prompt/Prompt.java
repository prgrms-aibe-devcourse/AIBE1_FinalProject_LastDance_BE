package store.lastdance.domain.prompt;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import store.lastdance.domain.common.BaseTimeEntity;

@Entity
@Table(name = "prompts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Prompt extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "prompt_key", unique = true, nullable = false)
    private String promptKey;

    @Column(name = "prompt_content", columnDefinition = "TEXT", nullable = false)
    private String promptContent;

    @Builder
    public Prompt(String promptKey, String promptContent) {
        this.promptKey = promptKey;
        this.promptContent = promptContent;
    }

    public void updateContent(String promptContent) {
        this.promptContent = promptContent;
    }
}
