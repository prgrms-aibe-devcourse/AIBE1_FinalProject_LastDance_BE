package store.lastdance.dto.community;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import store.lastdance.domain.community.PostCategory;

@Getter
@Setter
public class CreatePostRequestDTO {

    @NotBlank(message = "제목은 필수입니다.")
    @Size(max = 100, message = "제목은 100자 이내여야 합니다.")
    private String title;

    @NotBlank(message = "내용은 필수입니다.")
    @Size(max = 2000, message = "내용은 2000자 이내여야 합니다.")
    private String content;

    @NotNull(message = "카테고리는 필수입니다.")
    private PostCategory category;
}
