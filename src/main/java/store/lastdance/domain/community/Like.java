package store.lastdance.domain.community;

import lombok.*;
import jakarta.persistence.*;
import store.lastdance.domain.user.User;
import java.util.UUID;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "likes", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"post_id", "user_id"})
})
public class Like {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "like_id")
    private Long likeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Builder
    public Like(Post post, User user) {
        this.post = post;
        this.user = user;
    }

    public UUID getPostId() {
        return post.getPostId();
    }

    public UUID getUserId() {
        return user.getUserId();
    }
}
