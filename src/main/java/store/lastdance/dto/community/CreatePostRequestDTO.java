package store.lastdance.dto.community;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import store.lastdance.domain.community.PostCategory;

@Getter
@Setter
public class CreatePostRequestDTO {

    @NotBlank(message = "제목은 필수입니다.")
    private String title;

    @NotBlank(message = "내용은 필수입니다.")
    private String content;

    @NotNull(message = "카테고리는 필수입니다.")
    private PostCategory category;
}
