package store.lastdance.domain.game;

import lombok.*;
import jakarta.persistence.*;
import store.lastdance.domain.group.Group;
import store.lastdance.domain.user.User;
import store.lastdance.domain.common.BaseTimeEntity;

import java.util.List;

@Getter
@Entity
@Table(name = "game_results")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GameResult extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "result_id")
    private Long resultId;

    @Column(name = "game_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private GameType gameType;

    @Column(name = "participants", nullable = false)
    private List<String> participants;

    @Column(name = "result", nullable = false)
    private String result;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private Group group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "penalty", nullable = true)
    private String penalty;

    @Builder
    public GameResult(@NonNull GameType gameType, @NonNull List<String> participants, @NonNull String result, Group group, User user, String penalty) {
        this.gameType = gameType;
        this.participants = participants;
        this.result = result;
        this.group = group;
        this.user = user;
        this.penalty = penalty;
    }
}
