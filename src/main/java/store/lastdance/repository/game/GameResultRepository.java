package store.lastdance.repository.game;

import org.springframework.data.jpa.repository.JpaRepository;
import store.lastdance.domain.game.GameResult;
import store.lastdance.domain.group.Group;
import store.lastdance.domain.user.User;

import java.util.List;
import java.util.UUID;

public interface GameResultRepository extends JpaRepository<GameResult, Long> {
    List<GameResult> findByUser(User user);

    List<GameResult> findByGroup(Group group);
}
