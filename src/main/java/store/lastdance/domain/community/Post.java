package store.lastdance.domain.community;

import lombok.*;
import jakarta.persistence.*;
import org.hibernate.annotations.ColumnDefault;
import store.lastdance.domain.user.User;
import store.lastdance.domain.common.BaseTimeEntity;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;

@Getter
@Entity
@Table(name = "posts")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post extends BaseTimeEntity {
    @Id
    @Column(name = "post_id")
    private UUID postId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "category", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private PostCategory category;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "like_count", nullable = false)
    private Integer likeCount = 0;

    @Column(name = "report_count", nullable = false)
    private Integer reportCount = 0;

    @Column(name = "is_deleted", nullable = false)
    @ColumnDefault("false")
    private Boolean isDeleted = false;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL)
    private List<Like> likes = new ArrayList<>();

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL)
    private List<Bookmark> bookmarks = new ArrayList<>();

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL)
    private List<Comment> comments = new ArrayList<>();

    @Builder
    public Post(@NonNull UUID postId, @NonNull String title, @NonNull String content,
                @NonNull PostCategory category, @NonNull UUID userId) {
        this.postId = postId;
        this.title = title;
        this.content = content;
        this.category = category;
        this.userId = userId;
        this.likeCount = 0;
        this.reportCount = 0;
        this.isDeleted = false;
    }

    public void updateTitle(String title) {
        this.title = title;
    }

    public void updateContent(String content) {
        this.content = content;
    }

    public void updateCategory(PostCategory category) {
        this.category = category;
    }

    public void incrementLikeCount() {
        this.likeCount++;
    }

    public void decrementLikeCount() {
        if (this.likeCount > 0) {
            this.likeCount--;
        }
    }

    public void incrementReportCount() {
        this.reportCount++;
    }

    public void markAsDeleted() {
        this.isDeleted = true;
    }

}
