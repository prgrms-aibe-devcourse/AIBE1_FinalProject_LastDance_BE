package store.lastdance.domain.community;

public enum PostCategory {
    LIFE_TIPS("생활팁"),
    FREE_BOARD("자유게시판"),
    FIND_MATE("메이트구하기"),
    QNA("질문답변"),
    POLICY("정책게시판");

    private final String description;

    PostCategory(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}