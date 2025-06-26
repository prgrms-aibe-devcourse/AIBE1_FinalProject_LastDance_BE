package store.lastdance.repository.community;

import org.springframework.data.jpa.repository.JpaRepository;
import store.lastdance.domain.community.Like;
import store.lastdance.domain.community.Post;
import store.lastdance.domain.user.User;

import java.util.UUID;

public interface LikeRepository extends JpaRepository<Like, UUID> {
    boolean existsByPostAndUser(Post post, User user);
}
