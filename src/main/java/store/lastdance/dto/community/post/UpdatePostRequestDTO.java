package store.lastdance.dto.community.post;

import jakarta.validation.constraints.Size; // @NotBlank, @NotNull 제거
import lombok.Getter;
import lombok.Setter;
import store.lastdance.domain.community.PostCategory;

@Getter
@Setter
public class UpdatePostRequestDTO {

    // @NotBlank 제거: 제목이 필수가 아니게 됨
    @Size(max = 100, message = "제목은 100자 이내여야 합니다.")
    private String title;

    // @NotBlank 제거: 내용이 필수가 아니게 됨
    @Size(max = 2000, message = "내용은 2000자 이내여야 합니다.")
    private String content;

    // @NotNull 제거: 카테고리가 필수가 아니게 됨
    private PostCategory category;
}