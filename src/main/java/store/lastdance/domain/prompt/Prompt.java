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

    @Column(name = "prompt_type", unique = true, nullable = false) // Renamed to prompt_type
    private String promptType;

    @Column(name = "prompt_content", columnDefinition = "TEXT", nullable = false)
    private String promptContent;

    @Builder
    public Prompt(String promptType, String promptContent) { // Renamed parameter
        this.promptType = promptType;
        this.promptContent = promptContent;
    }

    public void updateContent(String promptContent) {
        this.promptContent = promptContent;
    }
}
